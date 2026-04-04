package org.blackum.blackaddons.feature.cheat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.core.util.ScoreboardUtils;
import org.blackum.blackaddons.feature.chat.ChatActionExecutor;
import org.blackum.blackaddons.feature.chat.ChatUtils;
import org.blackum.blackaddons.gui.render.DebugBoxRenderer;

public class FastLeap {
    private static final Pattern WITHER_DOOR_PATTERN = Pattern.compile("(?i)([A-Za-z0-9_]+) opened a .*?door!");
    private static final Pattern COOLDOWN_PATTERN = Pattern.compile("(?i)You are on a leap cooldown!");
    private static final Pattern BLOOD_DOOR_PATTERN = Pattern.compile("(?i)the BLOOD DOOR has been opened!");

    private static final int S1_SS_MIN_X = 106, S1_SS_MAX_X = 109, S1_SS_MIN_Y = 119, S1_SS_MAX_Y = 123, S1_SS_MIN_Z = 92,  S1_SS_MAX_Z = 95;
    private static final int S2_EE2_MIN_X  = 56,  S2_EE2_MAX_X  = 59,  S2_EE2_MIN_Y  = 108, S2_EE2_MAX_Y  = 111, S2_EE2_MIN_Z  = 129, S2_EE2_MAX_Z  = 132;
    private static final int S2_SAFE1_MIN_X = 68,  S2_SAFE1_MAX_X = 70,  S2_SAFE1_MIN_Y = 108, S2_SAFE1_MAX_Y = 111, S2_SAFE1_MIN_Z = 120, S2_SAFE1_MAX_Z = 122;
    private static final int S2_HEE2_MIN_X = 58,  S2_HEE2_MAX_X = 65,  S2_HEE2_MIN_Y = 130, S2_HEE2_MAX_Y = 135, S2_HEE2_MIN_Z = 135, S2_HEE2_MAX_Z = 146;
    private static final int S3_EE3_MIN_X  = 0,   S3_EE3_MAX_X  = 3,   S3_EE3_MIN_Y  = 108, S3_EE3_MAX_Y  = 111, S3_EE3_MIN_Z  = 100, S3_EE3_MAX_Z  = 106;
    private static final int S3_HEE3_MIN_X = 17,  S3_HEE3_MAX_X = 19,  S3_HEE3_MIN_Y = 121, S3_HEE3_MAX_Y = 123, S3_HEE3_MIN_Z = 89,  S3_HEE3_MAX_Z = 99;
    private static final int S4_MIN_X = 51, S4_MAX_X = 57, S4_MIN_Y = 115, S4_MAX_Y = 118, S4_MIN_Z = 48, S4_MAX_Z = 53;
    private static final int[] S1_SS = {S1_SS_MIN_X, S1_SS_MIN_Y, S1_SS_MIN_Z, S1_SS_MAX_X, S1_SS_MAX_Y, S1_SS_MAX_Z};
    private static final int[] S2_EE2 = {S2_EE2_MIN_X, S2_EE2_MIN_Y, S2_EE2_MIN_Z, S2_EE2_MAX_X, S2_EE2_MAX_Y, S2_EE2_MAX_Z};
    private static final int[] S2_SAFE1 = {S2_SAFE1_MIN_X, S2_SAFE1_MIN_Y, S2_SAFE1_MIN_Z, S2_SAFE1_MAX_X, S2_SAFE1_MAX_Y, S2_SAFE1_MAX_Z};
    private static final int[] S2_HEE2 = {S2_HEE2_MIN_X, S2_HEE2_MIN_Y, S2_HEE2_MIN_Z, S2_HEE2_MAX_X, S2_HEE2_MAX_Y, S2_HEE2_MAX_Z};
    private static final int[] S3_EE3 = {S3_EE3_MIN_X, S3_EE3_MIN_Y, S3_EE3_MIN_Z, S3_EE3_MAX_X, S3_EE3_MAX_Y, S3_EE3_MAX_Z};
    private static final int[] S3_HEE3 = {S3_HEE3_MIN_X, S3_HEE3_MIN_Y, S3_HEE3_MIN_Z, S3_HEE3_MAX_X, S3_HEE3_MAX_Y, S3_HEE3_MAX_Z};
    private static final int[] S4_ROOM = {S4_MIN_X, S4_MIN_Y, S4_MIN_Z, S4_MAX_X, S4_MAX_Y, S4_MAX_Z};

    private static final String CLASS_NONE = "NONE";
    private static final String INFINILEAP_NAME = "infinileap";
    private static final String SPIRIT_LEAP_NAME = "spirit leap";

    private static String leapTarget = null;
    private static boolean searchByClass = false;
    private static boolean inProgress = false;
    private static boolean clickedLeap = false;
    private static boolean menuOpened = false;
    private static boolean wasAttackDown = false;
    private static String lastDetectedRoom = null;
    private static long lastDetectedRoomAt = 0L;
    private static final Map<UUID, String> playerRooms = new HashMap<>();
    private static boolean bloodRoomOpened = false;
    private static String lastFailureReason = null;
    private static long lastFailureReasonAt = 0L;
    private static final long ROOM_MEMORY_MS = 3000L;

    public static void register() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> onChatMessage(message));
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> onChatMessage(message));
        ClientTickEvents.END_CLIENT_TICK.register(FastLeap::onTick);

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            resetState();
            bloodRoomOpened = false;
            leapTarget = null;
        });
    }

    private static void onChatMessage(Component message) {
        if (!ConfigManager.data.FastLeapEnabled) return;

        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.player == null) return;

            String cleanText = message.getString().replaceAll("(?i)§[0-9A-FK-ORX]", "").trim();

            if (BLOOD_DOOR_PATTERN.matcher(cleanText).find()) {
                bloodRoomOpened = true;
                leapTarget = null;
                searchByClass = false;
                debugMsg("[FastLeap] Blood Room opened. Door Opener disabled.");
                return;
            }

            Matcher doorMatcher = WITHER_DOOR_PATTERN.matcher(cleanText);
            if (doorMatcher.find()) {
                leapTarget = doorMatcher.group(1);
                searchByClass = false;
            }

            if (COOLDOWN_PATTERN.matcher(cleanText).find()) {
                resetState();
            }
            
            if (cleanText.contains("The Dungeon starts in 1 second.") || cleanText.contains("Starting in 1 second.")) {
                bloodRoomOpened = false;
                leapTarget = null;
                debugMsg("[FastLeap] Dungeon Start detected. Door Opener reset.");
            }
        });
    }

    private static String getActiveRoom(Minecraft mc) {
        String currentRoom = getDetectedRoom(mc);
        if (currentRoom != null) {
            lastDetectedRoom = currentRoom;
            lastDetectedRoomAt = System.currentTimeMillis();
            return currentRoom;
        }
        if (lastDetectedRoom != null && System.currentTimeMillis() - lastDetectedRoomAt <= ROOM_MEMORY_MS) {
            return lastDetectedRoom;
        }
        return null;
    }

    private static String getPositionalTargetPlayer(Minecraft mc) {
        if (mc.level == null || mc.player == null) return null;

        String myName = mc.player.getScoreboardName();
        String targetRoom = getActiveRoom(mc);
        if (targetRoom == null) return null;

        String configuredClass = getRoomClass(targetRoom);
        if (configuredClass == null || configuredClass.equals(CLASS_NONE)) return null;

        for (Player player : mc.level.players()) {
            if (player.getScoreboardName().equalsIgnoreCase(myName)) continue;

            double x = player.getX();
            double y = player.getY();
            double z = player.getZ();

            String zone = getZoneForPosition(x, y, z);
            boolean inRoom = targetRoom.equals(getStageFromZone(zone));

            if (inRoom) {
                String rawText = getPlayerClassRaw(player);
                String detectedClass = detectClass(rawText);

                if (detectedClass != null && configuredClass.equalsIgnoreCase(detectedClass)) {
                    String fullName = player.getName().getString();
                    debugMsg("[FastLeap] Priority MATCH! Target: " + fullName + " (" + detectedClass + ") in " + targetRoom);
                    return fullName;
                }
            }
        }
        return null;
    }

    private static String detectClass(String rawText) {
        if (rawText == null) return null;
        if (rawText.contains("[h] ")) return "HEALER";
        if (rawText.contains("[m] ")) return "MAGE";
        if (rawText.contains("[b] ")) return "BERSERK";
        if (rawText.contains("[a] ")) return "ARCHER";
        if (rawText.contains("[t] ")) return "TANK";
        // without space cuz idk which one works and cba to check XD
        // BLACKUM: bruh
        if (rawText.contains("[h]")) return "HEALER";
        if (rawText.contains("[m]")) return "MAGE";
        if (rawText.contains("[b]")) return "BERSERK";
        if (rawText.contains("[a]")) return "ARCHER";
        if (rawText.contains("[t]")) return "TANK";
        return null;
    }

    private static String getPlayerClassRaw(Player player) {
        String playerName = player.getName().getString().toLowerCase();

        for (String line : ScoreboardUtils.getCleanSidebarLines()) {
            String lowerLine = line.toLowerCase().replaceAll("§.", "").trim();

            if (lowerLine.matches("^\\s*\\[[hmbat]\\].*")) {
                int firstSpace = lowerLine.indexOf(' ');
                if (firstSpace == -1) continue;
                
                int secondSpace = lowerLine.indexOf(' ', firstSpace + 1);
                String rawChunk = secondSpace != -1 ? lowerLine.substring(firstSpace + 1, secondSpace) : lowerLine.substring(firstSpace + 1);
                
                String boardName = rawChunk.replaceAll("[^a-z0-9_]", "");
                
                if (boardName.length() >= 3 && playerName.startsWith(boardName)) {
                    debugMsg("[FastLeap] Scoreboard Scan Found: " + player.getName().getString() + " -> " + lowerLine);
                    return lowerLine;
                } else {
                    debugMsg("[FastLeap] Scoreboard Skip: " + playerName + " != " + boardName);
                }
            }
        }
        
        return null;
    }


    private static String getDetectedPlayerRoom(Player player) {
        if (player == null) return null;
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        return getStageFromZone(getZoneForPosition(x, y, z));
    }

    private static String getDetectedRoom(Minecraft mc) {
        return getDetectedPlayerRoom(mc.player);
    }

    private static String getRoomClass(String room) {
        return switch (room) {
            case "S2" -> ConfigManager.data.FastLeapS2Class;
            case "S3" -> ConfigManager.data.FastLeapS3Class;
            case "S4" -> ConfigManager.data.FastLeapS4Class;
            default -> null;
        };
    }


    private static String getZoneForPosition(double x, double y, double z) {
        if (isInBox(x, y, z, S1_SS_MIN_X,   S1_SS_MAX_X,   S1_SS_MIN_Y,   S1_SS_MAX_Y,   S1_SS_MIN_Z,   S1_SS_MAX_Z))   return "S1 SS";
        if (isInBox(x, y, z, S2_EE2_MIN_X,  S2_EE2_MAX_X,  S2_EE2_MIN_Y,  S2_EE2_MAX_Y,  S2_EE2_MIN_Z,  S2_EE2_MAX_Z))  return "S2 EE2";
        if (isInBox(x, y, z, S2_SAFE1_MIN_X, S2_SAFE1_MAX_X, S2_SAFE1_MIN_Y, S2_SAFE1_MAX_Y, S2_SAFE1_MIN_Z, S2_SAFE1_MAX_Z)) return "S2 safe-1";
        if (isInBox(x, y, z, S2_HEE2_MIN_X, S2_HEE2_MAX_X, S2_HEE2_MIN_Y, S2_HEE2_MAX_Y, S2_HEE2_MIN_Z, S2_HEE2_MAX_Z)) return "S2 HEE2";
        if (isInBox(x, y, z, S3_EE3_MIN_X,  S3_EE3_MAX_X,  S3_EE3_MIN_Y,  S3_EE3_MAX_Y,  S3_EE3_MIN_Z,  S3_EE3_MAX_Z))  return "S3 EE3";
        if (isInBox(x, y, z, S3_HEE3_MIN_X, S3_HEE3_MAX_X, S3_HEE3_MIN_Y, S3_HEE3_MAX_Y, S3_HEE3_MIN_Z, S3_HEE3_MAX_Z)) return "S3 HEE3";
        if (isInBox(x, y, z, S4_MIN_X,      S4_MAX_X,      S4_MIN_Y,      S4_MAX_Y,      S4_MIN_Z,      S4_MAX_Z))      return "S4";
        return null;
    }

    private static String getStageFromZone(String zone) {
        if (zone == null) return null;
        if (zone.startsWith("S1")) return "S1";
        if (zone.startsWith("S2")) return "S2";
        if (zone.startsWith("S3")) return "S3";
        if (zone.startsWith("S4")) return "S4";
        return null;
    }

    private static boolean isInBox(double x, double y, double z,
            int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    private static void onTick(Minecraft client) {
        if (!ConfigManager.data.FastLeapEnabled || client.player == null || client.level == null) return;

        if (ConfigManager.data.FastLeapPositional) {

            String myRoom = getDetectedRoom(client);
            if (myRoom != null && !myRoom.equals(lastDetectedRoom)) {
                String targetClass = getRoomClass(myRoom);
                debugMsg("[FastLeap] You entered " + myRoom
                        + (targetClass != null && !targetClass.equals(CLASS_NONE) ? " -> " + targetClass : ""));
            }
            if (myRoom != null) {
                lastDetectedRoom = myRoom;
                lastDetectedRoomAt = System.currentTimeMillis();
            }

            for (Player player : client.level.players()) {
                double px = player.getX(), py = player.getY(), pz = player.getZ();
                String zone = getZoneForPosition(px, py, pz);
                UUID uuid = player.getUUID();
                String lastZone = playerRooms.get(uuid);

                if (zone != null && !zone.equals(lastZone)) {
                    String label = zone.contains(" ") ? zone.split(" ", 2)[1] : "CORE";
                    debugMsg("[FastLeap] " + player.getName().getString() + " has been detected in " + label);
                }

                if (zone != null) playerRooms.put(uuid, zone);
                else playerRooms.remove(uuid);
            }
        }
        if (client.screen == null) {
            boolean attackDown = client.options.keyAttack.isDown();
            boolean useDown = client.options.keyUse.isDown();
            boolean simulatedAttack = ChatActionExecutor.getInstance().isKeySimulated(client.options.keyAttack);

            if (useDown) {
                inProgress = false;
            }

            if (attackDown && !wasAttackDown && !simulatedAttack) {
                if (!isLeapItem(client.player.getMainHandItem())) {
                    wasAttackDown = true;
                    return;
                }
                tryTriggerFromAttackAction(client, true);
            }
            wasAttackDown = attackDown;
        }

        if (client.screen instanceof ContainerScreen containerScreen) {
            String title = containerScreen.getTitle().getString();
            if (title.contains("Spirit Leap")) {
                if (!menuOpened) {
                    debugMsg("[FastLeap] Spirit Leap menu detected!");
                    menuOpened = true;
                }
                
                if (inProgress && leapTarget != null && !clickedLeap) {
                    int invStart = containerScreen.getMenu().slots.size() - 36;
                    String target = leapTarget.toLowerCase().trim();
                    int foundSlots = 0;

                    for (Slot slot : containerScreen.getMenu().slots) {
                        if (slot.index >= invStart) continue;
                        if (slot.getItem().isEmpty()) continue;
                        foundSlots++;

                        String itemName = slot.getItem().getHoverName().getString()
                                .replaceAll("(?i)§[0-9A-FK-ORX]", "").toLowerCase();

                        boolean matches = searchByClass
                                ? slotLoreContains(slot, target)
                                : itemName.startsWith(target) || itemName.contains(target);

                        if (matches) {
                            debugMsg("[FastLeap] Found " + itemName + "! Clicking slot " + slot.index);
                            clickedLeap = true;
                            client.gameMode.handleContainerInput(
                                    containerScreen.getMenu().containerId, slot.index, 0, ContainerInput.PICKUP, client.player);
                            finishLeap(client);
                            return;
                        }
                    }
                    if (foundSlots > 0 && !clickedLeap) {
                        debugMsg("[FastLeap] Scanned " + foundSlots + " items, no match for: " + target);
                    }
                }
            } else if (menuOpened) {
                resetState();
            }
        } else if (menuOpened) {
            resetState();
        }
    }

    public static boolean tryTriggerFromAttackAction(Minecraft client, boolean logFailure) {
        if (!ConfigManager.data.FastLeapEnabled || client == null || client.player == null || client.level == null) return false;
        if (client.screen != null || client.gameMode == null) return false;
        if (!isLeapItem(client.player.getMainHandItem())) return false;

        String target = null;
        boolean byClass = false;

        if (ConfigManager.data.FastLeapPositional) {
            target = getPositionalTargetPlayer(client);
        }

        if (target == null && ConfigManager.data.FastLeapDoorOpener && leapTarget != null && !bloodRoomOpened) {
            target = leapTarget;
            byClass = false;
        }

        if (target == null) {
            if (logFailure) {
                logFailureReason(buildFailureReason(client));
            }
            return false;
        }

        leapTarget = target;
        searchByClass = byClass;
        inProgress = true;
        clickedLeap = false;
        debugMsg("[FastLeap] Triggering! Target: " + target + (byClass ? " (Lore)" : " (Name)"));
        client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
        return true;
    }

    private static String getCleanItemName(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        return stack.getHoverName().getString().toLowerCase().replaceAll("(?i)§[0-9A-FK-ORX]", "");
    }

    private static boolean slotLoreContains(Slot slot, String target) {
        ItemLore lore = slot.getItem().get(DataComponents.LORE);
        if (lore == null) return false;
        for (Component line : lore.lines()) {
            if (line.getString().replaceAll("(?i)§[0-9A-FK-ORX]", "").toLowerCase().contains(target))
                return true;
        }
        return false;
    }

    private static void finishLeap(Minecraft client) {
        if (client.player != null) {
            client.player.closeContainer();
        }
        resetState();
    }

    private static void resetState() {
        inProgress = false;
        clickedLeap = false;
        menuOpened = false;
        searchByClass = false;
    }

    private static String buildFailureReason(Minecraft client) {
        if (ConfigManager.data.FastLeapPositional) {
            String currentRoom = getActiveRoom(client);
            if (currentRoom == null) {
                return "[FastLeap] Trigger failed: your position is not inside any configured S-room.";
            }

            String configuredClass = getRoomClass(currentRoom);
            if (configuredClass == null || configuredClass.equals(CLASS_NONE)) {
                return "[FastLeap] Trigger failed: " + currentRoom + " class is set to NONE.";
            }

            boolean foundPlayerInRoom = false;
            boolean foundClassMismatch = false;
            String myName = client.player.getScoreboardName();
            for (Player player : client.level.players()) {
                if (player.getScoreboardName().equalsIgnoreCase(myName)) continue;

                String playerRoom = getDetectedPlayerRoom(player);
                if (!currentRoom.equals(playerRoom)) continue;
                foundPlayerInRoom = true;

                String rawText = getPlayerClassRaw(player);
                String detectedClass = detectClass(rawText);
                if (detectedClass != null && configuredClass.equalsIgnoreCase(detectedClass)) {
                    return "[FastLeap] Trigger failed: positional target resolved unexpectedly.";
                }
                foundClassMismatch = true;
            }

            if (!foundPlayerInRoom) {
                return "[FastLeap] Trigger failed: no players found in " + currentRoom + ".";
            }

            if (foundClassMismatch) {
                return "[FastLeap] Trigger failed: no player in " + currentRoom + " matched configured class " + configuredClass + ".";
            }
        }

        if (ConfigManager.data.FastLeapDoorOpener && (leapTarget == null || bloodRoomOpened)) {
            return bloodRoomOpened
                    ? "[FastLeap] Trigger failed: Blood Door was opened, so door-opener target is disabled."
                    : "[FastLeap] Trigger failed: no door-opener target is stored.";
        }

        return "[FastLeap] Trigger failed: No player in boxes or no class match.";
    }

    private static void logFailureReason(String reason) {
        long now = System.currentTimeMillis();
        if (reason.equals(lastFailureReason) && now - lastFailureReasonAt < 750L) {
            return;
        }
        lastFailureReason = reason;
        lastFailureReasonAt = now;
        debugMsg(reason);
    }

    private static boolean isLeapItem(ItemStack stack) {
        String name = getCleanItemName(stack);
        return name.contains(INFINILEAP_NAME) || name.contains(SPIRIT_LEAP_NAME);
    }

    private static boolean isInfinileap(ItemStack stack) {
        return getCleanItemName(stack).contains(INFINILEAP_NAME);
    }

    private static void debugMsg(String msg) {
        if (ConfigManager.data.FastLeapDebug) {
            ChatUtils.send_debug(msg);
        }
    }

    public static List<String> getDebugInfo() {
        return new ArrayList<>();
    }

    public static List<DebugBoxRenderer.BoxSpec> getDebugBoxes() {
        List<DebugBoxRenderer.BoxSpec> boxes = new ArrayList<>();
        if (!ConfigManager.data.FastLeapDebug) return boxes;

        String activeRoom = null;
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            activeRoom = getActiveRoom(mc);
        }

        boxes.add(createStageBox("S1 Core", union(S1_SS), 0x3388FF, alphaForRoom("S1", activeRoom, 0.12f, 0.05f)));
        boxes.add(createStageBox("S2 Core", union(S2_EE2, S2_SAFE1, S2_HEE2), 0x33CC66, alphaForRoom("S2", activeRoom, 0.12f, 0.05f)));
        boxes.add(createStageBox("S3 Core", union(S3_EE3, S3_HEE3), 0xCCAA33, alphaForRoom("S3", activeRoom, 0.12f, 0.05f)));
        boxes.add(createStageBox("S4 Core", union(S4_ROOM), 0xCC5533, alphaForRoom("S4", activeRoom, 0.12f, 0.05f)));

        boxes.add(createStageBox("S1 SS", S1_SS, 0x66AAFF, alphaForRoom("S1", activeRoom, 0.22f, 0.10f)));
        boxes.add(createStageBox("S2 EE2", S2_EE2, 0x55DD88, alphaForRoom("S2", activeRoom, 0.18f, 0.08f)));
        boxes.add(createStageBox("S2 SAFE1", S2_SAFE1, 0x88FFAA, alphaForRoom("S2", activeRoom, 0.20f, 0.09f)));
        boxes.add(createStageBox("S2 HEE2", S2_HEE2, 0x33AA66, alphaForRoom("S2", activeRoom, 0.18f, 0.08f)));
        boxes.add(createStageBox("S3 EE3", S3_EE3, 0xFFDD55, alphaForRoom("S3", activeRoom, 0.18f, 0.08f)));
        boxes.add(createStageBox("S3 HEE3", S3_HEE3, 0xCCAA33, alphaForRoom("S3", activeRoom, 0.18f, 0.08f)));
        boxes.add(createStageBox("S4", S4_ROOM, 0xFF7744, alphaForRoom("S4", activeRoom, 0.20f, 0.09f)));
        return boxes;
    }

    private static DebugBoxRenderer.BoxSpec createStageBox(String label, int[] bounds, int color, float alpha) {
        int minX = Math.min(bounds[0], bounds[3]);
        int maxX = Math.max(bounds[0], bounds[3]) + 1;
        int minY = Math.min(bounds[1], bounds[4]);
        int maxY = Math.max(bounds[1], bounds[4]) + 1;
        int minZ = Math.min(bounds[2], bounds[5]);
        int maxZ = Math.max(bounds[2], bounds[5]) + 1;
        return new DebugBoxRenderer.BoxSpec(label, minX, minY, minZ, maxX, maxY, maxZ, color, alpha);
    }

    private static float alphaForRoom(String room, String activeRoom, float activeAlpha, float inactiveAlpha) {
        return room.equals(activeRoom) ? activeAlpha : inactiveAlpha;
    }

    private static int[] union(int[]... boxes) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (int[] box : boxes) {
            minX = Math.min(minX, Math.min(box[0], box[3]));
            minY = Math.min(minY, Math.min(box[1], box[4]));
            minZ = Math.min(minZ, Math.min(box[2], box[5]));
            maxX = Math.max(maxX, Math.max(box[0], box[3]));
            maxY = Math.max(maxY, Math.max(box[1], box[4]));
            maxZ = Math.max(maxZ, Math.max(box[2], box[5]));
        }
        return new int[]{minX, minY, minZ, maxX, maxY, maxZ};
    }
}
