package org.blackum.blackaddons.mixin.core;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import org.blackum.blackaddons.gui.screen.BaseScreen;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Window.class)
public class WindowMixin {
    @Inject(method = "calculateScale", at = @At("RETURN"), cancellable = true)
    private void onCalculateScale(int guiScale, boolean forceUnicode, CallbackInfoReturnable<Integer> cir) {
        if (Minecraft.getInstance().screen instanceof BaseScreen) {
            if (ConfigManager.data.forcedGuiScale > 0) {
                cir.setReturnValue(ConfigManager.data.forcedGuiScale);
            }
        }
    }
}
