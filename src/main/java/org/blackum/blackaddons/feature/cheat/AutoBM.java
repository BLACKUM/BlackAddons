package org.blackum.blackaddons.feature.cheat;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.core.util.TimeUtils;

import static org.blackum.blackaddons.core.util.MinecraftInstance.mc;

// GLORY TO BENJAMIN NETANYAHU 
public class AutoBM {
    public static void register() {
        ClientTickEvents.START_CLIENT_TICK.register(AutoBM::onClientTick);
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> handleChatMessage(message));
    }

    static TimeUtils timer = new TimeUtils();
    static boolean container_open = false;
    static boolean clicked = false;

    static int tick = 0;
    private static void onClientTick(Minecraft client) {
        tick++;

        if (!ConfigManager.data.autoBMConfig.AutoBMEnabled) {
            timer.reset();
            container_open = false;
            clicked = false;
            return;
        }

        if ((container_open || clicked) && !timer.isFinished(tick)) return;
        if (mc.gameMode == null) return;
        if (mc.player == null) return;
        if (!(mc.player.containerMenu instanceof ChestMenu chestMenu) || chestMenu.getRowCount() != 6) {
            container_open = false;
            return;
        }

        if (!container_open) {
            timer.startRandomTimer(tick, ConfigManager.data.autoBMConfig.min_fc_delay, ConfigManager.data.autoBMConfig.max_fc_delay);
            container_open = true;
            return;
        }

        if (clicked) clicked = false;

        var slot = mc.player.containerMenu.getSlot(13);
        var item = slot.getItem();
        if (item.getCustomName() == null)
            return;

        var name = item.getCustomName().getString();
        if (name.startsWith("Ballista Mechanic") && slot.mayPickup(mc.player)) {

            mc.gameMode.handleInventoryMouseClick(
                    mc.player.containerMenu.containerId,
                    slot.index,
                    0,
                    ClickType.PICKUP,
                    mc.player
            );

            timer.startRandomTimer(tick, ConfigManager.data.autoBMConfig.min_between_click_delay, ConfigManager.data.autoBMConfig.max_between_click_delay);
            clicked = true;
        }
    }

    private static void handleChatMessage(Component message) {
        if (message.getString().startsWith("You do not have enough tokens to upgrade this perk!") && container_open) {

            mc.player.closeContainer();
            container_open = false;
        }
    }


    public static class FeatureConfig {
        public boolean AutoBMEnabled = false;
        public float min_between_click_delay = 150.f;
        public float max_between_click_delay = 250.f;
        public float min_fc_delay = 500.f;
        public float max_fc_delay = 600.f;

        public FeatureConfig() {
            reset();
        }

        void reset() {
            AutoBMEnabled = false;
            min_between_click_delay = 150.f;
            max_between_click_delay = 200.f;

            min_fc_delay = 400.f;
            max_fc_delay = 500.f;
        }
    }
}
