package org.blackum.blackaddons.mixin.gui;

import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.blackum.blackaddons.core.manager.CustomNameManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerTabOverlay.class)
public class PlayerTabListMixin {

    @Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
    private void interceptTabName(PlayerInfo playerInfo, CallbackInfoReturnable<Component> cir) {
        Component original = cir.getReturnValue();
        if (original != null) {
            cir.setReturnValue(CustomNameManager.getInstance().replaceNames(original));
        }
    }
}
