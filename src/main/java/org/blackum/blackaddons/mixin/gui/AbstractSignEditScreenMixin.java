package org.blackum.blackaddons.mixin.gui;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.feature.modhider.ComponentUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import org.blackum.blackaddons.gui.notification.NotificationManager;
import org.blackum.blackaddons.gui.notification.NotificationType;
import org.blackum.blackaddons.core.util.Constants;

@Mixin(AbstractSignEditScreen.class)
public abstract class AbstractSignEditScreenMixin {
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
    @WrapOperation(method = "<init>(Lnet/minecraft/world/level/block/entity/SignBlockEntity;ZZLnet/minecraft/network/chat/Component;)V",
            at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;map(Ljava/util/function/Function;)Ljava/util/stream/Stream;"))
    private Stream<String> onInit(Stream<Component> instance, Function<Component, String> function, Operation<Stream<String>> original) {
        if (!ConfigManager.data.hideMods()) return original.call(instance, function);

        return original.call(instance, (Function<Component, String>) message -> {
            String processed = ComponentUtils.getString(message);
            if (!processed.equals(message.getString())) {
                Helper.showServerAttemptedReadingModsNotification();
            }
            return processed;
        });
    }
}
