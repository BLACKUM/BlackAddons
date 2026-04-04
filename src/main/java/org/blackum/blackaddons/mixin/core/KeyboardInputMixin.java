package org.blackum.blackaddons.mixin.core;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.KeyboardInput;
import org.blackum.blackaddons.core.util.AlignUtils;
import org.blackum.blackaddons.feature.cheat.Freecam;
import org.blackum.blackaddons.feature.chat.ChatActionExecutor;
import org.blackum.blackaddons.core.util.KeyBindingAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;isDown()Z"))
    private boolean onIsDown(KeyMapping keyMapping) {
        if (Freecam.getInstance().isActive()) {
            if (keyMapping instanceof KeyBindingAccessor accessor && accessor.blackaddons$isForced()) {
                return true;
            }
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (AlignUtils.shouldBlockMovementInput() && mc.options != null && isMovementKey(mc, keyMapping)) {
            return AlignUtils.isAllowedMovementKey(mc, keyMapping);
        }
        if (keyMapping instanceof KeyBindingAccessor accessor && accessor.blackaddons$isForced()) {
            return true;
        }
        return keyMapping.isDown();
    }

    private boolean isMovementKey(Minecraft mc, KeyMapping keyMapping) {
        return keyMapping == mc.options.keyUp
                || keyMapping == mc.options.keyDown
                || keyMapping == mc.options.keyLeft
                || keyMapping == mc.options.keyRight
                || keyMapping == mc.options.keyJump
                || keyMapping == mc.options.keyShift
                || keyMapping == mc.options.keySprint;
    }
}
