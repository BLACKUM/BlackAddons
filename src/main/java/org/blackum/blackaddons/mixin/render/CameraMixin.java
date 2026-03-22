package org.blackum.blackaddons.mixin.render;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.blackum.blackaddons.feature.cheat.Freecam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow private boolean detached;
    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Shadow protected abstract void setPosition(double x, double y, double z);

    @Inject(method = "setup", at = @At("TAIL"))
    private void onSetupTail(Level level, Entity entity, boolean detached, boolean flipped, float tickDelta, CallbackInfo ci) {
        if (Freecam.getInstance().isActive()) {
            this.detached = true;
            setPosition(
                Freecam.getInstance().getX(tickDelta),
                Freecam.getInstance().getY(tickDelta),
                Freecam.getInstance().getZ(tickDelta)
            );
            setRotation(
                Freecam.getInstance().getYaw(tickDelta),
                Freecam.getInstance().getPitch(tickDelta)
            );
        }
    }

    @Inject(method = "getMaxZoom(F)F", at = @At("HEAD"), cancellable = true)
    private void onGetMaxZoom(float startingDistance, CallbackInfoReturnable<Float> cir) {
        if (Freecam.getInstance().isActive()) {
            cir.setReturnValue(startingDistance);
        }
    }
}
