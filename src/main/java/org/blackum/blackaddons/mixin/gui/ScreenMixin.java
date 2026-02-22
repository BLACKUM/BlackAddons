package org.blackum.blackaddons.mixin.gui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.client.Minecraft;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
public class ScreenMixin {

    @Inject(method = "clickCommandAction", at = @At("HEAD"), cancellable = true)
    private static void onClickCommandAction(net.minecraft.client.player.LocalPlayer player, String command,
            Screen screen, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (ConfigManager.data.disableCommandConfirmation) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.player.connection != null) {
                if (command.startsWith("/")) {
                    command = command.substring(1);
                }
                mc.player.connection.sendCommand(command);
                ci.cancel();
            }
        }
    }
}
