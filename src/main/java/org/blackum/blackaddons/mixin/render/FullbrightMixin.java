package org.blackum.blackaddons.mixin.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.level.dimension.DimensionType;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LightTexture.class)
public class FullbrightMixin {
    
    @WrapOperation(method = "updateLightTexture",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;",
                    ordinal = 1))
    private Object wrapDarknessScale(OptionInstance<?> instance, Operation<Object> original) {
        return ConfigManager.data.legitFullbrightEnabled ? 0.0 : original.call(instance);
    }

    @WrapOperation(method = "updateLightTexture",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;",
                    ordinal = 2))
    private Object wrapGamma(OptionInstance<?> instance, Operation<Object> original) {
        return ConfigManager.data.legitFullbrightEnabled ? 10.0 : original.call(instance);
    }

    @WrapOperation(method = "updateLightTexture",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GameRenderer;getDarkenWorldAmount(F)F"))
    private float wrapDarkenWorld(GameRenderer renderer, float f, Operation<Float> original) {
        return ConfigManager.data.legitFullbrightEnabled ? 0.0F : original.call(renderer, f);
    }

    @WrapOperation(method = "updateLightTexture",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/dimension/DimensionType;ambientLight()F"))
    private float wrapAmbientLight(DimensionType type, Operation<Float> original) {
        return ConfigManager.data.legitFullbrightEnabled ? 1.0F : original.call(type);
    }

    @WrapOperation(method = "updateLightTexture",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/dimension/DimensionType;ambientLight()F",
                    ordinal = 0))
    private float wrapSkyFactor(DimensionType type, Operation<Float> original) {
        return ConfigManager.data.legitFullbrightEnabled ? 1.0F : original.call(type);
    }
}