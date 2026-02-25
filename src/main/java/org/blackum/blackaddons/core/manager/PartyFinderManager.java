package org.blackum.blackaddons.core.manager;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;

import com.google.gson.JsonObject;

import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.core.manager.ProfileStateManager;
import org.blackum.blackaddons.core.util.Constants;
import org.blackum.blackaddons.core.util.DungeonUtils;
import org.blackum.blackaddons.core.util.FormatUtils;
import org.blackum.blackaddons.core.util.JsonUtils;
import org.blackum.blackaddons.core.util.MinecraftInstance;
import org.blackum.blackaddons.feature.chat.ChatUtils;
import org.blackum.blackaddons.feature.dungeon.DungeonJoinHandler;
import org.blackum.blackaddons.gui.notification.NotificationManager;
import org.blackum.blackaddons.gui.notification.NotificationType;
import org.blackum.blackaddons.integration.BotIntegration;

public class PartyFinderManager {
    private static PartyFinderManager instance;
    private boolean inQueue = false;
    private final Map<String, Long> pendingRequests = new HashMap<>();
    private String currentPartyId = null;
    private final Set<String> partyMembers = new HashSet<>();
    private String currentFloor = null;

    private PartyFinderManager() {
    }

    public static synchronized PartyFinderManager getInstance() {
        if (instance == null) {
            instance = new PartyFinderManager();
        }
        return instance;
    }

    public boolean isInQueue() {
        return inQueue;
    }

    public void sendJoinRequest(String leaderName, String partyId) {
        if (MinecraftInstance.mc.player == null || MinecraftInstance.mc.player.connection == null)
            return;

        String msg = String.format(Constants.JOIN_REQUEST_TEMPLATE, partyId);

        MinecraftInstance.mc.player.connection.sendCommand("msg " + leaderName + " " + msg);
        pendingRequests.put(leaderName, System.currentTimeMillis());

        MinecraftInstance.mc.execute(() -> {
            MinecraftInstance.mc.gui.getChat().addMessage(ChatUtils.getMessage("Sent join request to " + leaderName));
        });
    }

    public void onChatMessage(Component message) {
        String text = message.getString();

        Matcher requestMatcher = Pattern.compile(Constants.JOIN_REQUEST_WHISPER_REGEX)
                .matcher(text);
        if (requestMatcher.find()) {
            String sender = requestMatcher.group(1) != null ? requestMatcher.group(1) : requestMatcher.group(2);
            String requestId = requestMatcher.group(3);

            if (currentPartyId != null && !currentPartyId.equals(requestId)) {
                return;
            }

            MinecraftInstance.mc.execute(() -> {
                MinecraftInstance.mc.gui.getChat()
                        .addMessage(ChatUtils.getMessage("§aJoin request from " + sender));
            });

            if (ConfigManager.data.partyFinderAutoInvite) {
                MinecraftInstance.mc.execute(() -> {
                    if (MinecraftInstance.mc.player != null && MinecraftInstance.mc.player.connection != null) {
                        MinecraftInstance.mc.player.connection.sendCommand("party invite " + sender);
                    }
                });
            } else {
                MinecraftInstance.mc.execute(() -> {
                    MinecraftInstance.mc.gui.getChat()
                            .addMessage(ChatUtils.getPrefix()
                                    .append(Component.literal(sender + " " + Constants.MSG_WANTS_TO_JOIN)));
                    MinecraftInstance.mc.gui.getChat().addMessage(Component.literal("§7  ")
                            .append(Component.literal(Constants.LABEL_INVITE)
                                    .withStyle(
                                            s -> s.withClickEvent(new ClickEvent.RunCommand("/party invite " + sender))
                                                    .withHoverEvent(new HoverEvent.ShowText(
                                                            Component.literal(Constants.HOVER_INVITE))))));
                });
                if (ConfigManager.data.partyFinderShowStatsOnRequest) {
                    DungeonJoinHandler.fetchAndShowStats(sender, "N/A", "0");
                }
            }
            return;
        }

        Matcher inviteMatcher = Pattern.compile(Constants.PARTY_INVITE_REGEX)
                .matcher(text);
        if (inviteMatcher.find()) {
            String sender = inviteMatcher.group(1);
            String leader = inviteMatcher.group(2);
            String matchedRequest = null;

            if (pendingRequests.containsKey(sender)) {
                matchedRequest = sender;
            } else if (leader != null && pendingRequests.containsKey(leader)) {
                matchedRequest = leader;
            }

            if (ConfigManager.data.partyFinderAutoAcceptInvite && matchedRequest != null) {
                long time = pendingRequests.get(matchedRequest);
                if (System.currentTimeMillis() - time < 60000) {
                    MinecraftInstance.mc.execute(() -> {
                        MinecraftInstance.mc.gui.getChat()
                                .addMessage(ChatUtils.getMessage(String.format(Constants.MSG_AUTO_ACCEPT, sender)));
                        LocalPlayer player = MinecraftInstance.mc.player;
                        if (player != null && player.connection != null) {
                            player.connection.sendCommand("p accept " + sender);
                        }
                    });
                    pendingRequests.remove(matchedRequest);
                }
            }
            return;
        }

        Matcher joinMatcher = Pattern.compile(Constants.PARTY_JOINED_REGEX)
                .matcher(text);
        if (joinMatcher.find()) {
            String name = joinMatcher.group(1);
            partyMembers.add(name);
            refreshPartyDescription();
            return;
        }

        Matcher leftMatcher = Pattern.compile(Constants.PARTY_LEFT_REGEX).matcher(text);
        Matcher removedMatcher = Pattern.compile(Constants.PARTY_REMOVED_REGEX)
                .matcher(text);
        if (leftMatcher.find() || removedMatcher.find()) {
            String name = leftMatcher.find() ? leftMatcher.group(1)
                    : (removedMatcher.find() ? removedMatcher.group(1) : null);
            if (name != null) {
                partyMembers.remove(name);
                refreshPartyDescription();
            }
            return;
        }

        if (Pattern.compile(Constants.PARTY_JOINED_OTHERS_REGEX).matcher(text).find() ||
                Pattern.compile(Constants.PARTY_DISBANDED_REGEX).matcher(text).find() ||
                Pattern.compile(Constants.PARTY_LEAVE_REGEX).matcher(text).find()) {
            partyMembers.clear();
            inQueue = false;
            currentFloor = null;
            currentPartyId = null;
            return;
        }
    }

    private void refreshPartyDescription() {
        if (!inQueue || currentFloor == null)
            return;

        List<CompletableFuture<String>> futures = new ArrayList<>();

        for (String member : partyMembers) {
            futures.add(ProfileStateManager.getInstance().getProfile(member, null, false).thenApply(res -> {
                if (res != null && res.isSuccess()) {
                    JsonObject data = res.getData();
                    double cataLvl = DungeonUtils.getCataLevel(JsonUtils.getDouble(data, "catacombs"));
                    return member + ": " + String.format("%.1f", cataLvl);
                }
                return member;
            }));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenAccept(v -> {
                    String memberStats = String.join(", ", futures.stream().map(f -> f.join()).toList());
                    String note = "Members: " + memberStats;
                    if (note.length() > 200)
                        note = note.substring(0, 197) + "...";

                    BotIntegration.updateParty(partyMembers.size(), note);
                });
    }

    public void createParty(String floor, JsonObject reqs) {
        if (inQueue) {
            NotificationManager.addNotification(Constants.PARTY_FINDER_TITLE, Constants.MSG_ALREADY_IN_QUEUE,
                    NotificationType.WARNING);
            return;
        }

        this.currentFloor = floor;
        this.partyMembers.clear();
        this.partyMembers.add(MinecraftInstance.mc.getUser().getName());

        String player = MinecraftInstance.mc.getUser().getName();
        ProfileStateManager.getInstance().getProfile(player, null, false).thenAccept(res -> {
            String statsNote = Constants.DEFAULT_PARTY_NOTE;
            if (res != null && res.isSuccess()) {
                JsonObject data = res.getData();
                double cataLvl = DungeonUtils.getCataLevel(JsonUtils.getDouble(data, "catacombs"));

                JsonObject classes = JsonUtils.getObject(data, "classes");
                double bestClassLvl = 0;
                if (classes != null) {
                    for (String cls : classes.keySet()) {
                        double lvl = DungeonUtils.getCataLevel(classes.get(cls).getAsDouble());
                        if (lvl > bestClassLvl) {
                            bestClassLvl = lvl;
                        }
                    }
                }

                JsonObject floors = JsonUtils.getObject(data, "floors");
                String pbStr = "N/A";
                if (floors != null && floors.has(floor)) {
                    JsonObject fData = floors.getAsJsonObject(floor);
                    int pbMs = JsonUtils.getInt(fData, "fastest_s_plus");
                    if (pbMs <= 0)
                        pbMs = JsonUtils.getInt(fData, "fastest_s");
                    if (pbMs > 0)
                        pbStr = FormatUtils.formatMs(pbMs);
                }

                statsNote = String.format("Cata: %.2f Class: %.2f PB: %s", cataLvl, bestClassLvl, pbStr);
            }

            String finalNote = statsNote;
            BotIntegration.createParty(floor, finalNote, reqs, 5).thenAccept(response -> {
                if (response != null && response.has("status")
                        && response.get("status").getAsString().equals("success")) {
                    inQueue = true;
                    if (response.has("id")) {
                        currentPartyId = response.get("id").getAsString();
                    }
                    if (MinecraftInstance.mc.player != null && MinecraftInstance.mc.player.connection != null) {
                        MinecraftInstance.mc.gui.getChat()
                                .addMessage(ChatUtils.getMessage(String.format(Constants.MSG_QUEUE_STARTED, floor)));
                    }
                } else {
                    String error = response != null && response.has("error") ? response.get("error").getAsString()
                            : "Unknown error";
                    NotificationManager.addNotification(Constants.PARTY_FINDER_TITLE,
                            "Failed to create party: " + error,
                            NotificationType.ERROR);
                }
            });
        });
    }

    public void unqueue() {
        if (!inQueue)
            return;

        BotIntegration.unqueueParty().thenAccept(success -> {
            if (success) {
                inQueue = false;
                currentPartyId = null;
                MinecraftInstance.mc.execute(() -> {
                    MinecraftInstance.mc.gui.getChat()
                            .addMessage(ChatUtils.getMessage(Constants.MSG_REMOVED_QUEUE));
                });
            } else {
                NotificationManager.addNotification(Constants.PARTY_FINDER_TITLE, "Failed to unqueue.",
                        NotificationType.ERROR);
            }
        });
    }
}
