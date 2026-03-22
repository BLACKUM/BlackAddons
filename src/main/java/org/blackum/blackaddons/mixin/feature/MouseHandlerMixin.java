package org.blackum.blackaddons.mixin.feature;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.blackum.blackaddons.feature.cheat.Freecam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @Inject(method = "onButton", at = @At("TAIL"))
    private void blackaddons$trackMouseButtons(long window, MouseButtonInfo button, int action, CallbackInfo ci) {
        Freecam.getInstance().setMouseButtonState(button.button(), action != 0);
    }
}
