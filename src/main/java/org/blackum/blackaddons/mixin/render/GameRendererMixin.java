package org.blackum.blackaddons.mixin.render;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.feature.cheat.Freecam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "getNightVisionScale", at = @At("RETURN"), cancellable = true)
    private static void onGetNightVisionScale(LivingEntity entity, float tickDelta, CallbackInfoReturnable<Float> cir) {
        if (ConfigManager.data.legitFullbrightEnabled) {
            cir.setReturnValue(0.0F);
        }
    }

    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void onRenderItemInHand(CallbackInfo ci) {
        if (Freecam.getInstance().isActive() && !ConfigManager.data.freecamShowHands) {
            ci.cancel();
        }
    }
}
