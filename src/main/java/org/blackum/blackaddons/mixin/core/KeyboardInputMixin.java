package org.blackum.blackaddons.mixin.core;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.KeyboardInput;
import org.blackum.blackaddons.feature.cheat.Freecam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;isDown()Z"))
    private boolean onIsDown(KeyMapping keyMapping) {
        if (Freecam.getInstance().isActive()) {
            return false;
        }
        return keyMapping.isDown();
    }
}
