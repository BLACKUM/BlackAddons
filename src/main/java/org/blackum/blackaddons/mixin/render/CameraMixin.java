package org.blackum.blackaddons.mixin.render;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import org.blackum.blackaddons.feature.cheat.Freecam;
import org.blackum.blackaddons.feature.cheat.Perspective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow private boolean detached;
    @Shadow private Entity entity;
    @Shadow protected abstract void setRotation(float yRot, float xRot);
    @Shadow protected abstract void setPosition(double x, double y, double z);
    @Shadow protected abstract void move(float forwards, float up, float right);
    @Shadow protected abstract float getMaxZoom(float startingDistance);

    @Inject(method = "alignWithEntity", at = @At("TAIL"))
    private void onAlignWithEntity(float tickDelta, CallbackInfo ci) {
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
        } else if (Perspective.getInstance().isActive()) {
            this.detached = true;
            net.minecraft.world.phys.Vec3 eyePos = this.entity.getEyePosition(tickDelta);
            setPosition(eyePos.x, eyePos.y, eyePos.z);
            
            float pYaw = Perspective.getInstance().getYaw(tickDelta);
            float pPitch = Perspective.getInstance().getPitch(tickDelta);
            setRotation(pYaw, pPitch);

            float dist = Perspective.getInstance().getDistance();
            move(-getMaxZoom(dist), 0.0F, 0.0F);
        }
    }

    @Inject(method = "getMaxZoom(F)F", at = @At("HEAD"), cancellable = true)
    private void onGetMaxZoom(float startingDistance, CallbackInfoReturnable<Float> cir) {
        if (Freecam.getInstance().isActive() || Perspective.getInstance().isActive()) {
            cir.setReturnValue(startingDistance);
        }
    }
}
