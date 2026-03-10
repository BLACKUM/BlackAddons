package org.blackum.blackaddons.mixin.gui;

import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.feature.modhider.ComponentUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Set;
import org.blackum.blackaddons.gui.notification.NotificationManager;
import org.blackum.blackaddons.gui.notification.NotificationType;
import org.blackum.blackaddons.core.util.Constants;

@Mixin(AbstractSignEditScreen.class)
public abstract class AbstractSignEditScreenMixin {
    @Shadow private String[] messages;

    public static class Helper {
        private static final Set<String> serversAttemptedReadingMods = new HashSet<>();

        public static void showServerAttemptedReadingModsNotification() {
            Minecraft mc = Minecraft.getInstance();
            ServerData serverData = mc.getCurrentServer();
            if (serverData != null) {
                String ip = serverData.ip;
                if (serversAttemptedReadingMods.contains(ip)) {
                    return;
                }
                serversAttemptedReadingMods.add(ip);
            }

            mc.execute(() -> NotificationManager.addNotification(
                    Constants.MOD_DETECTION_TITLE,
                    Constants.MOD_DETECTION_MESSAGE,
                    NotificationType.WARNING));
        }
    }

    @Inject(method = "<init>(Lnet/minecraft/world/level/block/entity/SignBlockEntity;ZZLnet/minecraft/network/chat/Component;)V", at = @At("TAIL"))
    private void onInit(SignBlockEntity signBlockEntity, boolean front, boolean filtered, Component title, CallbackInfo ci) {
        if (!ConfigManager.data.hideMods()) return;

        SignText text = signBlockEntity.getText(front);
        for (int i = 0; i < messages.length; i++) {
            Component message = text.getMessage(i, filtered);
            String processed = ComponentUtils.getString(message);
            if (!processed.equals(message.getString())) {
                Helper.showServerAttemptedReadingModsNotification();
                messages[i] = processed;
            }
        }
    }
}
