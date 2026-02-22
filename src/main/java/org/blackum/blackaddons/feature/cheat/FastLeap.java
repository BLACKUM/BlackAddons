package org.blackum.blackaddons.feature.cheat;

import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.core.config.ConfigManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FastLeap {
    private static final String PREFIX = ChatFormatting.GRAY + "[" + ChatFormatting.GOLD + "FastLeap" + ChatFormatting.GRAY + "] ";
    private static final Pattern WITHER_DOOR_PATTERN = Pattern.compile("(?i)(?:\\[.*?\\] )?([A-Za-z0-9_]+) opened a (?:Wither )?door!");
    private static final Pattern COOLDOWN_PATTERN = Pattern.compile("(?i)You are on a leap cooldown!");
    
    private static String lastOpener = null;
    private static boolean inProgress = false;
    private static boolean clickedLeap = false;
    private static boolean menuOpened = false;

    private static boolean wasAttackDown = false;

    public static void register() {
        Blackaddons.LOGGER.info("Registering FastLeap (Stability Mode)...");
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> onChatMessage(message));
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> onChatMessage(message));
        ClientTickEvents.END_CLIENT_TICK.register(FastLeap::onTick);
    }

    private static void onChatMessage(Component message) {
        if (!ConfigManager.data.FastLeapEnabled) return;

        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.player == null) return;
            
            String rawText = message.getString();
            String cleanText = rawText.replaceAll("(?i)§[0-9A-FK-ORX]", "").trim();

            Matcher doorMatcher = WITHER_DOOR_PATTERN.matcher(cleanText);
            if (doorMatcher.find()) {
                lastOpener = doorMatcher.group(1);
            }

            if (COOLDOWN_PATTERN.matcher(cleanText).find()) {
                resetState();
            }
        });
    }

    private static void onTick(Minecraft client) {
        if (!ConfigManager.data.FastLeapEnabled || client.player == null) return;

        if (client.screen == null) {
            boolean attackDown = client.options.keyAttack.isDown();
            boolean useDown = client.options.keyUse.isDown();

            // Right-click always cancels auto-leap intent
            if (useDown) {
                inProgress = false;
            }

            // Trigger on left-click if we have a target
            if (attackDown && !wasAttackDown && lastOpener != null) {
                inProgress = true;
                clickedLeap = false;
                
                if (client.gameMode != null) {
                    client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
                }
            }
            wasAttackDown = attackDown;
        }

        if (client.screen instanceof ContainerScreen containerScreen) {
            String title = containerScreen.getTitle().getString();
            if ("Spirit Leap".equals(title)) {
                menuOpened = true;
                if (inProgress && lastOpener != null && !clickedLeap) {
                    int invStart = containerScreen.getMenu().slots.size() - 36;
                    
                    for (Slot slot : containerScreen.getMenu().slots) {
                        if (slot.index >= invStart) continue;
                        
                        Component nameComp = slot.getItem().getHoverName();
                        String name = nameComp.getString().replaceAll("(?i)§[0-9A-FK-ORX]", "").toLowerCase();
                        
                        if (name.startsWith(lastOpener.toLowerCase())) {
                            clickedLeap = true;
                            client.gameMode.handleInventoryMouseClick(containerScreen.getMenu().containerId, slot.index, 0, ClickType.PICKUP, client.player);
                            client.player.displayClientMessage(Component.literal(PREFIX + ChatFormatting.GREEN + "Leaping to " + lastOpener), false);
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
    }

    public static List<String> getDebugInfo() {
        return new ArrayList<>();
    }
}
