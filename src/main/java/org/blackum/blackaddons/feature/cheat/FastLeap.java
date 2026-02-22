package org.blackum.blackaddons.feature.cheat;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.blackum.blackaddons.core.config.ConfigManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FastLeap {
    private static final String PREFIX = ChatFormatting.DARK_GREEN + "[" + ChatFormatting.GREEN + "FastLeap" + ChatFormatting.DARK_GREEN + "] ";
    private static final Pattern WITHER_DOOR_PATTERN = Pattern.compile("(?i).*?\\b([a-zA-Z0-9_]{3,16})\\s+opened\\s+a\\s+.*WITHER.*\\s+door!");
    private static final Pattern COOLDOWN_PATTERN = Pattern.compile("(?i)This\\s+ability\\s+is\\s+on\\s+cooldown\\s+for\\s+(\\d+)s\\.");

    private static String lastOpener = null;
    private static final List<String> leapQueue = Collections.synchronizedList(new ArrayList<>());
    private static boolean menuOpened = false;
    private static boolean inProgress = false;
    private static boolean clickedLeap = false;
    private static boolean wasAttackDown = false;
    private static boolean wasUseDown = false;

    public static void register() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> onChatMessage(message));
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> onChatMessage(message));
        
        ClientTickEvents.END_CLIENT_TICK.register(FastLeap::onTick);
    }

    private static void onChatMessage(Component message) {
        if (!ConfigManager.data.FastLeapEnabled) return;

        // Strip ALL color codes, remove non-ASCII, and normalize spaces
        String text = message.getString().replaceAll("(?i)§[0-9A-FK-ORX]", "")
                .replaceAll("[^\\x20-\\x7E]", "")
                .replaceAll("\\s+", " ")
                .trim();
        Minecraft mc = Minecraft.getInstance();

        mc.execute(() -> {
            if (mc.player == null) return;

            if (text.toLowerCase().contains("door")) {
                mc.player.displayClientMessage(Component.literal(PREFIX + ChatFormatting.LIGHT_PURPLE + "RAW: " + ChatFormatting.WHITE + text), false);
            }

            Matcher doorMatcher = WITHER_DOOR_PATTERN.matcher(text);
            if (doorMatcher.find()) {
                lastOpener = doorMatcher.group(1);
                mc.player.displayClientMessage(Component.literal(PREFIX + ChatFormatting.YELLOW + "Door opener: " + ChatFormatting.WHITE + lastOpener), false);
            }

            Matcher cooldownMatcher = COOLDOWN_PATTERN.matcher(text);
            if (cooldownMatcher.find()) {
                clickedLeap = false;
                inProgress = false;
                synchronized(leapQueue) {
                    if (!leapQueue.isEmpty()) {
                        leapQueue.remove(leapQueue.size() - 1);
                    }
                }
            }
        });
    }

    private static void onTick(Minecraft client) {
        if (!ConfigManager.data.FastLeapEnabled || client.player == null) return;

        if (client.screen == null) {
            boolean attackDown = client.options.keyAttack.isDown();
            boolean useDown = client.options.keyUse.isDown();

            ItemStack mainHand = client.player.getMainHandItem();
            boolean holdingLeap = "INFINITE_SPIRIT_LEAP".equals(getHeldItemID(mainHand));

            if (useDown && !wasUseDown && holdingLeap) {
                clearQueue();
            }

            if (attackDown && !wasAttackDown && holdingLeap && !inProgress) {
                String leapTo = getLeap(client);
                client.player.displayClientMessage(Component.literal(PREFIX + ChatFormatting.YELLOW + "Click detected. LeapTo=" + ChatFormatting.WHITE + (leapTo.isEmpty() ? "EMPTY" : leapTo) + ChatFormatting.GRAY + " [DoorOpener=" + ConfigManager.data.FastLeapDoorOpener + ", lastOpener=" + lastOpener + "]"), false);
                if (leapTo != null && !leapTo.isEmpty()) {
                    queueLeap(leapTo);
                    inProgress = true;
                    client.execute(() -> {
                        if (client.player != null && client.screen == null) {
                            client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
                        }
                    });
                }
            }

            wasAttackDown = attackDown;
            wasUseDown = useDown;
        }

        if (client.screen instanceof ContainerScreen containerScreen) {
            String title = containerScreen.getTitle().getString();
            if ("Spirit Leap".equals(title)) {
                menuOpened = true;

                if (leapQueue.isEmpty()) {
                    inProgress = false;
                } else if (!clickedLeap) {
                    String targetLeap = leapQueue.get(0);
                    int playerInvStart = containerScreen.getMenu().slots.size() - 36;
                    client.player.displayClientMessage(Component.literal(PREFIX + ChatFormatting.YELLOW + "Scanning GUI for: " + ChatFormatting.WHITE + targetLeap + ChatFormatting.GRAY + " (slots 0-" + (playerInvStart - 1) + ")"), false);

                    boolean matched = false;
                    for (Slot slot : containerScreen.getMenu().slots) {
                        if (slot.index >= playerInvStart) continue;

                        ItemStack stack = slot.getItem();
                        if (!stack.isEmpty()) {
                            String itemName = stack.getHoverName().getString().replaceAll("(?i)§[0-9A-FK-OR]", "").toLowerCase();
                            client.player.displayClientMessage(Component.literal(PREFIX + ChatFormatting.GRAY + "  slot " + slot.index + ": " + itemName), false);
                            if (itemName.equals(targetLeap.toLowerCase())) {
                                int windowId = containerScreen.getMenu().containerId;

                                client.execute(() -> {
                                    if (!clickedLeap && client.player != null && client.screen instanceof ContainerScreen currentScreen) {
                                        if (currentScreen.getMenu().containerId == windowId) {
                                            clickedLeap = true;
                                            client.gameMode.handleInventoryMouseClick(windowId, slot.index, 0, ClickType.PICKUP, client.player);
                                            client.player.displayClientMessage(Component.literal(PREFIX + ChatFormatting.GREEN + "Leaping to " + ChatFormatting.RED + targetLeap), false);
                                            reloadGUI(client);
                                        }
                                    }
                                });
                                break;
                            }
                        }
                    }
                    if (!matched) {
                        client.player.displayClientMessage(Component.literal(PREFIX + ChatFormatting.RED + "No match found for: " + targetLeap), false);
                    }
                }
            } else {
                menuOpened = false;
            }
        } else {
            menuOpened = false;
        }
    }

    private static void reloadGUI(Minecraft client) {
        menuOpened = false;
        synchronized(leapQueue) {
            if (!leapQueue.isEmpty()) {
                leapQueue.remove(0);
            }
        }
        inProgress = false;
        clickedLeap = false;
        client.execute(() -> {
            if (client.screen instanceof ContainerScreen cs && "Spirit Leap".equals(cs.getTitle().getString())) {
                client.setScreen(null);
            }
        });
    }

    public static void clearQueue() {
        leapQueue.clear();
        inProgress = false;
        clickedLeap = false;
    }

    public static void queueLeap(String name) {
        if (name != null && !name.isEmpty()) {
            leapQueue.add(name);
        }
    }

    private static String getLeap(Minecraft client) {
        String leapString = "";

        if (ConfigManager.data.FastLeapDoorOpener && lastOpener != null) {
            leapString = lastOpener;
        }

        if (ConfigManager.data.FastLeapPositional && client.player != null) {
            if (isPlayerInBox(client.player, 113, 160, 48, 89, 100, 122)) {
                leapString = ConfigManager.data.FastLeapS1;
            } else if (isPlayerInBox(client.player, 91, 160, 145, 19, 100, 121)) {
                leapString = ConfigManager.data.FastLeapS2;
            } else if (isPlayerInBox(client.player, -6, 160, 123, 19, 100, 50)) {
                leapString = ConfigManager.data.FastLeapS3;
            } else if (isPlayerInBox(client.player, 17, 160, 27, 90, 100, 50)) {
                leapString = ConfigManager.data.FastLeapS4;
            }
        }

        return leapString;
    }

    private static boolean isPlayerInBox(Player player, double x1, double y1, double z1, double x2, double y2, double z2) {
        AABB box = new AABB(Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2), 
                            Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2));
        return box.contains(player.position());
    }

    private static String getHeldItemID(ItemStack stack) {
        if (stack.isEmpty()) return "";
        String name = stack.getHoverName().getString().replaceAll("(?i)§[0-9A-FK-OR]", "");
        if (name.contains("Infinileap") || name.contains("Infinite Spirit Leap")) {
            return "INFINITE_SPIRIT_LEAP";
        }
        return "";
    }

    public static List<String> getDebugInfo() {
        List<String> info = new ArrayList<>();
        if (!ConfigManager.data.FastLeapEnabled) return info;

        info.add("");
        info.add(ChatFormatting.AQUA + "[FastLeap Debug]");
        info.add("InProgress: " + inProgress);
        info.add("ClickedLeap: " + clickedLeap);
        info.add("MenuOpened: " + menuOpened);
        info.add("Queue: " + leapQueue);
        info.add("LastOpener: " + lastOpener);

        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            String held = getHeldItemID(client.player.getMainHandItem());
            info.add("HeldID: " + (held.isEmpty() ? "none" : held));
            info.add("Screen: " + (client.screen != null ? client.screen.getClass().getSimpleName() : "none"));
        }

        return info;
    }
}
