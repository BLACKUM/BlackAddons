package org.blackum.blackaddons.feature.cheat;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class BloodBlink {
    private static KeyMapping blinkKey;
    private static int macroPhase = 0; // 0: Idle, 1: AOTV, 2: Pearls
    private static int pearlClicksRemaining = 0;
    private static int resetTicks = -1;

    public static void register() {
        blinkKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.blackaddons.bloodblink",
                GLFW.GLFW_KEY_V,
                "category.blackaddons.cheats"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(BloodBlink::onTick);
    }

    private static void onTick(Minecraft client) {
        if (client.player == null || client.gameMode == null) return;

        // Shift Key Handling
        if (resetTicks > 0) {
            resetTicks--;
            if (resetTicks == 0) {
                client.options.keyShift.setDown(false);
            }
        }

        // Key Listen
        while (blinkKey.consumeClick()) {
            if (!ConfigManager.data.BloodBlinkEnabled) continue;
            macroPhase = 1;
        }

        // Macro Logic
        if (macroPhase == 1) {
            executeAOTVPase(client);
            macroPhase = 2;
            pearlClicksRemaining = 7;
        } else if (macroPhase == 2) {
            executePearlPhase(client);
        }
    }

    private static void executeAOTVPase(Minecraft client) {
        // 1. Look at coordinates
        client.player.setXRot(48.9f); // Pitch
        client.player.setYRot(-29.3f); // Yaw

        // 2. Switch to AOTV
        int slot = findItemSlot(client, "aspect of the void");
        if (slot != -1) {
            client.player.getInventory().selectedSlot = slot;
        }

        // 3. Sneak
        client.options.keyShift.setDown(true);
        resetTicks = 10; // Keep shift down for duration of macro

        // 4. Click
        client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
    }

    private static void executePearlPhase(Minecraft client) {
        // 1. Change Yaw
        client.player.setYRot(-90f);

        // 2. Switch to Pearls
        int slot = findItemSlot(client, "ender pearl");
        if (slot != -1) {
            client.player.getInventory().selectedSlot = slot;
        }

        // 3. Click 7 times (we do them as fast as possible, but we can spread them if needed)
        // Spreading them over ticks is safer for registration
        if (pearlClicksRemaining == 0) {
            pearlClicksRemaining = 7;
        }

        if (pearlClicksRemaining > 0) {
            client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
            pearlClicksRemaining--;
        }

        if (pearlClicksRemaining == 0) {
            macroPhase = 0;
            resetTicks = 2; // Short buffer to release shift
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
        return new ArrayList<>();
    }
}
