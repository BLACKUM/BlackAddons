package org.blackum.blackaddons.feature.cheat;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.core.util.LocationUtils;
import org.blackum.blackaddons.feature.chat.ChatUtils;
import org.blackum.blackaddons.mixin.core.InventoryAccessor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class BloodBlink {
    private static final KeyMapping.Category CHEATS_CATEGORY = KeyMapping.Category
            .register(ResourceLocation.fromNamespaceAndPath("blackaddons", "main"));

    private static KeyMapping blinkKey;
    private static int macroPhase = 0;
    private static int pearlClicksRemaining = 0;
    private static int resetTicks = -1;

    public static void register() {
        blinkKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.blackaddons.bloodblink",
                GLFW.GLFW_KEY_UNKNOWN,
                CHEATS_CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(BloodBlink::onTick);
    }

    private static void onTick(Minecraft client) {
        if (client.player == null || client.gameMode == null || client.screen != null || !LocationUtils.inDungeons())
            return;

        if (resetTicks > 0) {
            resetTicks--;
            if (resetTicks == 0) {
                client.options.keyShift.setDown(false);
            }
        }

        while (blinkKey.consumeClick()) {
            if (!ConfigManager.data.BloodBlinkEnabled)
                continue;
            ChatUtils.send_debug("BloodBlink Triggered!");
            macroPhase = 1;
        }

        if (macroPhase == 1) {
            executeAOTVPhase(client);
            macroPhase = 2;
            pearlClicksRemaining = 7;
        } else if (macroPhase == 2) {
            executePearlPhase(client);
        }
    }

    private static void executeAOTVPhase(Minecraft client) {
        ChatUtils.send_debug("Starting AOTV Phase...");
        client.player.setXRot(48.9f);
        client.player.setYRot(-29.3f);

        int slot = findItemSlot(client, "aspect of the void");
        if (slot != -1) {
            ((InventoryAccessor) client.player.getInventory()).setBlackaddonsSelected(slot);
        } else {
            ChatUtils.send_debug("AOTV Error: Item not found!");
        }

        client.options.keyShift.setDown(true);
        resetTicks = 10;

        client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
    }

    private static void executePearlPhase(Minecraft client) {
        if (pearlClicksRemaining == 7) {
            ChatUtils.send_debug("Starting Pearl Phase...");
        }
        client.player.setYRot(-90f);

        int slot = findItemSlot(client, "ender pearl");
        if (slot != -1) {
            ((InventoryAccessor) client.player.getInventory()).setBlackaddonsSelected(slot);
        } else {
            ChatUtils.send_debug("Pearl Error: Item not found!");
            macroPhase = 0;
            return;
        }

        if (pearlClicksRemaining > 0) {
            client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
            pearlClicksRemaining--;
        }

        if (pearlClicksRemaining == 0) {
            ChatUtils.send_debug("Macro Complete!");
            macroPhase = 0;
            resetTicks = 2;
        }
    }

    private static int findItemSlot(Minecraft client, String namePart) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getItem(i);
            String name = stack.getHoverName().getString().toLowerCase();
            if (name.contains(namePart.toLowerCase())) {
                return i;
            }
        }
        return -1;
    }

    public static List<String> getDebugInfo() {
        List<String> info = new ArrayList<>();
        if (!ConfigManager.data.BloodBlinkEnabled)
            return info;

        info.add("");
        info.add(ChatFormatting.DARK_RED + "[BloodBlink Debug]");
        info.add("Location: " + LocationUtils.getLocation());
        info.add("Dungeon: " + (LocationUtils.inDungeons() ? ChatFormatting.GREEN + "YES"
                : ChatFormatting.RED + "NO"));

        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            boolean hasAOTV = findItemSlot(client, "aspect of the void") != -1;
            boolean hasPearls = findItemSlot(client, "ender pearl") != -1;
            info.add("Items: "
                    + (hasAOTV ? ChatFormatting.GREEN + "AOTV"
                            : ChatFormatting.RED + "NO AOTV")
                    + ChatFormatting.RESET + " / "
                    + (hasPearls ? ChatFormatting.GREEN + "Pearls"
                            : ChatFormatting.RED + "NO Pearls"));
        }

        String phaseStr = switch (macroPhase) {
            case 1 -> ChatFormatting.GOLD + "AOTV PHASE";
            case 2 -> ChatFormatting.LIGHT_PURPLE + "PEARL PHASE";
            default -> "Idle";
        };
        info.add("Phase: " + phaseStr);

        if (macroPhase == 0) {
            info.add("Target: 48.9 / -29.3 -> -90.0");
        } else if (macroPhase == 2) {
            info.add("Pearl Clicks: " + pearlClicksRemaining);
        }

        if (resetTicks > 0) {
            info.add("Sneak Ticks: " + resetTicks);
        }

        return info;
    }
}
