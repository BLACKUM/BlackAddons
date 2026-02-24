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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FastLeap {
    private static final Pattern WITHER_DOOR_PATTERN = Pattern.compile("(?i)(?:\\[.*?\\] )?([A-Za-z0-9_]+) opened a (?:Wither )?door!");
    private static final Pattern COOLDOWN_PATTERN = Pattern.compile("(?i)You are on a leap cooldown!");
    private static final Pattern BLOOD_DOOR_PATTERN = Pattern.compile("(?i)the BLOOD DOOR has been opened!");

    private static final int S1_MIN_X = 108, S1_MAX_X = 112, S1_MIN_Y = 118, S1_MAX_Y = 122, S1_MIN_Z = 92, S1_MAX_Z = 96;
    private static final int S2_MIN_X = 58, S2_MAX_X = 62, S2_MIN_Y = 130, S2_MAX_Y = 134, S2_MIN_Z = 136, S2_MAX_Z = 140;
    private static final int S3_MIN_X = 0, S3_MAX_X = 4, S3_MIN_Y = 107, S3_MAX_Y = 111, S3_MIN_Z = 102, S3_MAX_Z = 106;
    private static final int S4_MIN_X = 52, S4_MAX_X = 56, S4_MIN_Y = 113, S4_MAX_Y = 117, S4_MIN_Z = 48, S4_MAX_Z = 52;

    private static final String CLASS_NONE = "NONE";

    private static String leapTarget = null;
    private static boolean searchByClass = false;
    private static boolean inProgress = false;
    private static boolean clickedLeap = false;
    private static boolean menuOpened = false;
    private static boolean wasAttackDown = false;
    private static String lastDetectedRoom = null;

    public static void register() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> onChatMessage(message));
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> onChatMessage(message));
        ClientTickEvents.END_CLIENT_TICK.register(FastLeap::onTick);
    }

    private static void onChatMessage(Component message) {
        if (!ConfigManager.data.FastLeapEnabled) return;

        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.player == null) return;

            String cleanText = message.getString().replaceAll("(?i)§[0-9A-FK-ORX]", "").trim();

            if (BLOOD_DOOR_PATTERN.matcher(cleanText).find()) {
                leapTarget = null;
                searchByClass = false;
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
        });
    }

    private static String getDetectedRoom(Minecraft mc) {
        if (mc.player == null) return null;
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        if (isInBox(x, y, z, S1_MIN_X, S1_MAX_X, S1_MIN_Y, S1_MAX_Y, S1_MIN_Z, S1_MAX_Z)) return "S1";
        if (isInBox(x, y, z, S2_MIN_X, S2_MAX_X, S2_MIN_Y, S2_MAX_Y, S2_MIN_Z, S2_MAX_Z)) return "S2";
        if (isInBox(x, y, z, S3_MIN_X, S3_MAX_X, S3_MIN_Y, S3_MAX_Y, S3_MIN_Z, S3_MAX_Z)) return "S3";
        if (isInBox(x, y, z, S4_MIN_X, S4_MAX_X, S4_MIN_Y, S4_MAX_Y, S4_MIN_Z, S4_MAX_Z)) return "S4";
        return null;
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


    private static boolean isInBox(double x, double y, double z,
            int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    private static void onTick(Minecraft client) {
        if (!ConfigManager.data.FastLeapEnabled || client.player == null) return;

        if (ConfigManager.data.FastLeapPositional) {
            String room = getDetectedRoom(client);
            if (room != null && !room.equals(lastDetectedRoom)) {
                String target = getRoomClass(room);
                ChatUtils.send_debug("[FastLeap] Entered " + room
                        + (target != null && !target.equals("NONE") ? " -> " + target : ""));
            }
            lastDetectedRoom = room;
        }
        if (client.screen == null) {
            boolean attackDown = client.options.keyAttack.isDown();
            boolean useDown = client.options.keyUse.isDown();

            if (useDown) {
                inProgress = false;
            }

            if (attackDown && !wasAttackDown) {
                String target = null;
                boolean byClass = false;

                if (ConfigManager.data.FastLeapPositional) {
                    String room = getDetectedRoom(client);
                    String positional = room != null ? getRoomClass(room) : null;
                    if (positional != null && !positional.isEmpty() && !positional.equals(CLASS_NONE)) {
                        target = positional;
                        byClass = true;
                    }
                }

                if (target == null && ConfigManager.data.FastLeapDoorOpener && leapTarget != null) {
                    target = leapTarget;
                    byClass = false;
                }

                if (target != null) {
                    leapTarget = target;
                    searchByClass = byClass;
                    inProgress = true;
                    clickedLeap = false;
                    if (client.gameMode != null) {
                        client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
                    }
                }
            }
            wasAttackDown = attackDown;
        }

        if (client.screen instanceof ContainerScreen containerScreen) {
            String title = containerScreen.getTitle().getString();
            if ("Spirit Leap".equals(title)) {
                menuOpened = true;
                if (inProgress && leapTarget != null && !clickedLeap) {
                    int invStart = containerScreen.getMenu().slots.size() - 36;
                    String target = leapTarget.toLowerCase();

                    for (Slot slot : containerScreen.getMenu().slots) {
                        if (slot.index >= invStart) continue;
                        if (slot.getItem().isEmpty()) continue;

                        boolean matches = searchByClass
                                ? slotLoreContains(slot, target)
                                : slot.getItem().getHoverName().getString()
                                        .replaceAll("(?i)§[0-9A-FK-ORX]", "").toLowerCase()
                                        .startsWith(target);

                        if (matches) {
                            clickedLeap = true;
                            client.gameMode.handleInventoryMouseClick(
                                    containerScreen.getMenu().containerId, slot.index, 0, ClickType.PICKUP, client.player);
                            finishLeap(client);
                            break;
                        }
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

    public static List<String> getDebugInfo() {
        return new ArrayList<>();
    }
}
