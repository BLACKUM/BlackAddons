package org.blackum.blackaddons.mixin.core;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.blackum.blackaddons.feature.waypoint.WaypointActionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "setPosRaw(DDD)V", at = @At("TAIL"))
    private void onSetPosRaw(double x, double y, double z, CallbackInfo ci) {
        if ((Object) this instanceof LocalPlayer) {
            WaypointActionManager.getInstance().onPlayerPositionChanged();
        }
    }
}
