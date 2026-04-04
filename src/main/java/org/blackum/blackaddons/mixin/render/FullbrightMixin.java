package org.blackum.blackaddons.mixin.render;

import net.minecraft.client.renderer.state.LightmapRenderState;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.joml.Vector3f;

@Mixin(LightmapRenderStateExtractor.class)
public class FullbrightMixin {
    
    @Inject(method = "extract", at = @At("TAIL"))
    private void onExtract(LightmapRenderState renderState, float partialTicks, CallbackInfo ci) {
        if (ConfigManager.data.legitFullbrightEnabled) {
            renderState.brightness = 10.0F;
            renderState.darknessEffectScale = 0.0F;
            renderState.bossOverlayWorldDarkening = 0.0F;
            renderState.skyFactor = 1.0F;
            renderState.ambientColor = new Vector3f(1.0F, 1.0F, 1.0F);
        }
    }
}