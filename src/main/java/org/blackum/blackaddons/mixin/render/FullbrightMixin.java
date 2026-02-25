package org.blackum.blackaddons.mixin.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.renderer.LightTexture;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LightTexture.class)
public class FullbrightMixin {

    @Redirect(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;"))
    private Object redirectGamma(OptionInstance<Double> instance) {
        if (ConfigManager.data.legitFullbrightEnabled
                && instance == Minecraft.getInstance().options.gamma()) {
            return 1000.0;
        }
        return instance.get();
    }
}
