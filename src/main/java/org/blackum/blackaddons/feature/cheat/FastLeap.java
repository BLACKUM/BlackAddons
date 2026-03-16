package org.blackum.blackaddons.feature.cheat;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.component.ItemLore;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.feature.chat.ChatUtils;
import org.blackum.blackaddons.core.util.ScoreboardUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.item.ItemStack;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FastLeap {
    private static final Pattern WITHER_DOOR_PATTERN = Pattern.compile("(?i)([A-Za-z0-9_]+) opened a .*?door!");
    private static final Pattern COOLDOWN_PATTERN = Pattern.compile("(?i)You are on a leap cooldown!");
    private static final Pattern BLOOD_DOOR_PATTERN = Pattern.compile("(?i)the BLOOD DOOR has been opened!");

    private static final int S1_SS_MIN_X = 106, S1_SS_MAX_X = 109, S1_SS_MIN_Y = 119, S1_SS_MAX_Y = 123, S1_SS_MIN_Z = 92,  S1_SS_MAX_Z = 95;
    private static final int S2_EE2_MIN_X  = 56,  S2_EE2_MAX_X  = 59,  S2_EE2_MIN_Y  = 108, S2_EE2_MAX_Y  = 111, S2_EE2_MIN_Z  = 129, S2_EE2_MAX_Z  = 132;
    private static final int S2_SAFE1_MIN_X = 69,  S2_SAFE1_MAX_X = 69,  S2_SAFE1_MIN_Y = 109, S2_SAFE1_MAX_Y = 110, S2_SAFE1_MIN_Z = 121, S2_SAFE1_MAX_Z = 121;
    private static final int S2_HEE2_MIN_X = 58,  S2_HEE2_MAX_X = 65,  S2_HEE2_MIN_Y = 130, S2_HEE2_MAX_Y = 135, S2_HEE2_MIN_Z = 135, S2_HEE2_MAX_Z = 146;
    private static final int S3_EE3_MIN_X  = 0,   S3_EE3_MAX_X  = 3,   S3_EE3_MIN_Y  = 108, S3_EE3_MAX_Y  = 111, S3_EE3_MIN_Z  = 100, S3_EE3_MAX_Z  = 106;
    private static final int S3_HEE3_MIN_X = 17,  S3_HEE3_MAX_X = 19,  S3_HEE3_MIN_Y = 121, S3_HEE3_MAX_Y = 123, S3_HEE3_MIN_Z = 89,  S3_HEE3_MAX_Z = 99;
    private static final int S4_MIN_X = 51, S4_MAX_X = 57, S4_MIN_Y = 115, S4_MAX_Y = 118, S4_MIN_Z = 48, S4_MAX_Z = 53;

    private static final String CLASS_NONE = "NONE";

    private static String leapTarget = null;
    private static boolean searchByClass = false;
    private static boolean inProgress = false;
    private static boolean clickedLeap = false;
    private static boolean menuOpened = false;
    private static boolean wasAttackDown = false;
    private static String lastDetectedRoom = null;
    private static final Map<UUID, String> playerRooms = new HashMap<>();
    private static boolean bloodRoomOpened = false;

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
                ChatUtils.send_debug("[FastLeap] Blood Room opened. Door Opener disabled.");
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
                ChatUtils.send_debug("[FastLeap] Dungeon Start detected. Door Opener reset.");
            }
        });
    }

    private static String getPositionalTargetPlayer(Minecraft mc) {
        if (mc.level == null || mc.player == null) return null;

        String myName = mc.player.getScoreboardName();
        String[] roomPriority = {"S1", "S2", "S3", "S4"};

        for (String targetRoom : roomPriority) {
            String configuredClass = getRoomClass(targetRoom);
            if (configuredClass == null || configuredClass.equals(CLASS_NONE)) continue;

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
                    
                    if (detectedClass != null && configuredClass != null && configuredClass.equalsIgnoreCase(detectedClass)) {
                        String fullName = player.getName().getString();
                        ChatUtils.send_debug("[FastLeap] Priority MATCH! Target: " + fullName + " (" + detectedClass + ") in " + targetRoom);
                        return fullName;
                    }
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
                    ChatUtils.send_debug("[FastLeap] Scoreboard Scan Found: " + player.getName().getString() + " -> " + lowerLine);
                    return lowerLine;
                } else {
                    ChatUtils.send_debug("[FastLeap] Scoreboard Skip: " + playerName + " != " + boardName);
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
            case "S1" -> ConfigManager.data.FastLeapS1Class;
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
                ChatUtils.send_debug("[FastLeap] You entered " + myRoom
                        + (targetClass != null && !targetClass.equals(CLASS_NONE) ? " -> " + targetClass : ""));
            }
            lastDetectedRoom = myRoom;

            for (Player player : client.level.players()) {
                double px = player.getX(), py = player.getY(), pz = player.getZ();
                String zone = getZoneForPosition(px, py, pz);
                UUID uuid = player.getUUID();
                String lastZone = playerRooms.get(uuid);

                if (zone != null && !zone.equals(lastZone)) {
                    String label = zone.contains(" ") ? zone.split(" ", 2)[1] : "CORE";
                    ChatUtils.send_debug("[FastLeap] " + player.getName().getString() + " has been detected in " + label);
                }

                if (zone != null) playerRooms.put(uuid, zone);
                else playerRooms.remove(uuid);
            }
        }
        if (client.screen == null) {
            boolean attackDown = client.options.keyAttack.isDown();
            boolean useDown = client.options.keyUse.isDown();

            if (useDown) {
                inProgress = false;
            }

            if (attackDown && !wasAttackDown) {
                ItemStack hand = client.player.getMainHandItem();
                if (!isLeapItem(hand)) {
                    wasAttackDown = true;
                    return;
                }

                String target = null;
                boolean byClass = false;

                if (ConfigManager.data.FastLeapPositional) {
                    target = getPositionalTargetPlayer(client);
                }

                if (target == null && ConfigManager.data.FastLeapDoorOpener && leapTarget != null && !bloodRoomOpened) {
                    target = leapTarget;
                    byClass = false;
                }

                if (target != null) {
                    leapTarget = target;
                    searchByClass = byClass;
                    inProgress = true;
                    clickedLeap = false;
                    if (client.gameMode != null) {
                        ChatUtils.send_debug("[FastLeap] Triggering! Target: " + target + (byClass ? " (Lore)" : " (Name)"));
                        client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
                    }
                } else {
                    ChatUtils.send_debug("[FastLeap] Trigger failed: No player in boxes or no class match.");
                }
            }
            wasAttackDown = attackDown;
        }

        if (client.screen instanceof ContainerScreen containerScreen) {
            String title = containerScreen.getTitle().getString();
            if (title.contains("Spirit Leap")) {
                if (!menuOpened) {
                    ChatUtils.send_debug("[FastLeap] Spirit Leap menu detected!");
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
                            ChatUtils.send_debug("[FastLeap] Found " + itemName + "! Clicking slot " + slot.index);
                            clickedLeap = true;
                            client.gameMode.handleInventoryMouseClick(
                                    containerScreen.getMenu().containerId, slot.index, 0, ClickType.PICKUP, client.player);
                            finishLeap(client);
                            return;
                        }
                    }
                    if (foundSlots > 0 && !clickedLeap) {
                        ChatUtils.send_debug("[FastLeap] Scanned " + foundSlots + " items, no match for: " + target);
                    }
                }
            } else if (menuOpened) {
                resetState();
            }
        } else if (menuOpened) {
            resetState();
        }
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

    private static boolean isLeapItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        String name = stack.getHoverName().getString().toLowerCase().replaceAll("(?i)§[0-9A-FK-ORX]", "");
        return name.contains("infinileap");
    }

    public static List<String> getDebugInfo() {
        return new ArrayList<>();
    }
}
