package org.blackum.blackaddons.mixin.core;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.blackum.blackaddons.core.manager.CustomNameManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class PlayerNameMixin {

    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void interceptDisplayName(CallbackInfoReturnable<Component> cir) {
        Component original = cir.getReturnValue();
        if (original != null) {
            Component replaced = CustomNameManager.getInstance().replaceNames(original);
            cir.setReturnValue(replaced);
        }
    }
}
