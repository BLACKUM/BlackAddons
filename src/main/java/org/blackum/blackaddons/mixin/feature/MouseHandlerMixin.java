package org.blackum.blackaddons.mixin.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.blackum.blackaddons.feature.cheat.FastLeap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Inject(method = "onPress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getOverlay()Lnet/minecraft/client/gui/screens/Overlay;", shift = At.Shift.BEFORE), cancellable = true)
    private void onMousePress(long window, int button, int action, int modifiers, CallbackInfo ci) {
        if (action == 1) {
            Minecraft client = Minecraft.getInstance();
            if (client.screen == null) {
                boolean cancel = FastLeap.handleMouseClick(client, button);
                if (cancel) {
                    ci.cancel();
                }
            }
        }
    }
}
