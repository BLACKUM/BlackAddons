package org.blackum.blackaddons.mixin.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.QuadParticleRenderState;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.mixin.core.ParticleAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SingleQuadParticle.class)
public abstract class SingleQuadParticleMixin {

    @Inject(method = "extract", at = @At("HEAD"), cancellable = true)
    private void onExtract(QuadParticleRenderState state, Camera camera, float f, CallbackInfo ci) {
        if (ConfigManager.data.disableNearbyParticles) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                ParticleAccessor accessor = (ParticleAccessor) this;
                double px = accessor.getX();
                double py = accessor.getY();
                double pz = accessor.getZ();
                
                double dx = px - mc.player.getX();
                double dy = py - mc.player.getY();
                double dz = pz - mc.player.getZ();
                
                if (dx * dx + dy * dy + dz * dz < 4.0) {
                    ci.cancel();
                }
            }
        }
    }
}
