package org.blackum.blackaddons.feature.cheat;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
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
    
    private static boolean inProgress = false;
    private static boolean clickedLeap = false;
    private static boolean menuOpened = false;
    
    private static boolean wasAttackDown = false;
    private static boolean wasUseDown = false;

    public static void register() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> onChatMessage(message));
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> onChatMessage(message));
        ClientTickEvents.END_CLIENT_TICK.register(FastLeap::onTick);
    }

    private static void onChatMessage(Component message) {
        if (!ConfigManager.data.FastLeapEnabled) return;

        String text = message.getString().replaceAll("(?i)§[0-9A-FK-ORX]", "")
                .replaceAll("[^\\x20-\\x7E]", "")
                .replaceAll("\\s+", " ")
                .trim();
        Minecraft mc = Minecraft.getInstance();

        mc.execute(() -> {
            if (mc.player == null) return;

            Matcher doorMatcher = WITHER_DOOR_PATTERN.matcher(text);
            if (doorMatcher.find()) {
                lastOpener = doorMatcher.group(1);
                mc.player.displayClientMessage(Component.literal(PREFIX + ChatFormatting.YELLOW + "Door opener: " + ChatFormatting.WHITE + lastOpener), false);
            }

            Matcher cooldownMatcher = COOLDOWN_PATTERN.matcher(text);
            if (cooldownMatcher.find()) {
                clearQueue();
            }
        });
    }

    private static void onTick(Minecraft client) {
        if (!ConfigManager.data.FastLeapEnabled || client.player == null) return;

        if (client.screen == null) {
            boolean attackDown = client.options.keyAttack.isDown();
            boolean useDown = client.options.keyUse.isDown();

            ItemStack mainHand = client.player.getMainHandItem();
            boolean holdingLeap = isHoldingLeap(mainHand);

            if (useDown && !wasUseDown && holdingLeap) {
                clearQueue();
            }

            if (attackDown && !wasAttackDown && holdingLeap && !inProgress) {
                if (ConfigManager.data.FastLeapDoorOpener && lastOpener != null) {
                    inProgress = true;
                    leapQueue.clear();
                    leapQueue.add(lastOpener);
                    
                    if (client.gameMode != null) {
                        client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
                    }
                }
            }

            wasAttackDown = attackDown;
            wasUseDown = useDown;
        }

        if (client.screen instanceof ContainerScreen containerScreen) {
            String title = containerScreen.getTitle().getString();
            if ("Spirit Leap".equals(title)) {
                menuOpened = true;
                if (!leapQueue.isEmpty() && !clickedLeap) {
                    String target = leapQueue.get(0);
                    int invStart = containerScreen.getMenu().slots.size() - 36;
                    
                    for (Slot slot : containerScreen.getMenu().slots) {
                        if (slot.index >= invStart) continue;
                        
                        ItemStack stack = slot.getItem();
                        if (!stack.isEmpty()) {
                            String name = stack.getHoverName().getString()
                                    .replaceAll("(?i)§[0-9A-FK-OR]", "")
                                    .toLowerCase();
                            
                            if (name.startsWith(target.toLowerCase())) {
                                clickedLeap = true;
                                int windowId = containerScreen.getMenu().containerId;
                                
                                client.gameMode.handleInventoryMouseClick(windowId, slot.index, 0, ClickType.PICKUP, client.player);
                                client.player.displayClientMessage(Component.literal(PREFIX + ChatFormatting.GREEN + "Leaping to " + ChatFormatting.RED + target), false);
                                
                                finishLeap(client);
                                break;
                            }
                        }
                    }
                }
            } else if (menuOpened) {
                clearQueue();
                menuOpened = false;
            }
        } else if (menuOpened) {
            clearQueue();
            menuOpened = false;
        }
    }

    private static void finishLeap(Minecraft client) {
        leapQueue.clear();
        inProgress = false;
        clickedLeap = false;
        menuOpened = false;
        client.setScreen(null);
    }

    public static void clearQueue() {
        leapQueue.clear();
        inProgress = false;
        clickedLeap = false;
    }

    private static boolean isHoldingLeap(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String name = stack.getHoverName().getString()
                .replaceAll("(?i)§[0-9A-FK-ORX]", "")
                .trim()
                .toLowerCase();
        return name.contains("infinileap") || name.contains("infinite spirit leap");
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
            info.add("Holding Leap: " + isHoldingLeap(client.player.getMainHandItem()));
            info.add("Screen: " + (client.screen != null ? client.screen.getClass().getSimpleName() : "none"));
        }

        return info;
    }
}
