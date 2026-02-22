package org.blackum.blackaddons.mixin.core;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import org.blackum.blackaddons.core.manager.CustomNameManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerTeam.class)
public class PlayerTeamMixin {

    @Inject(method = "formatNameForTeam", at = @At("RETURN"), cancellable = true)
    private static void interceptFormatNameForTeam(Team team, Component name,
            CallbackInfoReturnable<MutableComponent> cir) {
        MutableComponent original = cir.getReturnValue();
        if (original != null) {
            Component replaced = CustomNameManager.getInstance().replaceNames(original);
            cir.setReturnValue(replaced.copy());
        }
    }
}
