package org.blackum.blackaddons.mixin.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.gui.render.ColorUtils;
import org.blackum.blackaddons.mixin.core.ParticleAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SingleQuadParticle.class)
public abstract class SingleQuadParticleMixin {
    @Shadow
    protected float rCol;
    @Shadow
    protected float gCol;
    @Shadow
    protected float bCol;

    @Inject(method = "extract(Lnet/minecraft/client/renderer/state/level/QuadParticleRenderState;Lnet/minecraft/client/Camera;F)V", at = @At("HEAD"), cancellable = true)
    private void onExtract(QuadParticleRenderState state, Camera camera, float partialTick, CallbackInfo ci) {
        if (ConfigManager.data.RainbowParticles) {
            int color = ColorUtils.getRainbow(5, 0.5f, 1.0f);
            this.rCol = ((color >> 16) & 0xFF) / 255.0f;
            this.gCol = ((color >> 8) & 0xFF) / 255.0f;
            this.bCol = (color & 0xFF) / 255.0f;
        }

        if (ConfigManager.data.disableNearbyParticles) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                ParticleAccessor accessor = (ParticleAccessor) (Object) this;
                double px = accessor.getX();
                double py = accessor.getY();
                double pz = accessor.getZ();
                
                double dx = px - mc.player.getX();
                double dy = py - mc.player.getY();
                double dz = pz - mc.player.getZ();
                
                if (dx * dx + dy * dy + dz * dz < 9.0) {
                    ci.cancel();
                }
            }
        }
    }
}
