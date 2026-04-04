package org.blackum.blackaddons.mixin.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.blackum.blackaddons.feature.waypoint.WaypointActionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Shadow
    public Screen screen;

    @Shadow
    public abstract void resizeGui();

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void onBeforeSetScreen(Screen screen, CallbackInfo ci) {
        if (this.screen != null && screen == null) {
            WaypointActionManager.getInstance().onGuiClosed();
        }
    }

    @Inject(method = "setScreen", at = @At("TAIL"))
    private void onSetScreen(Screen screen, CallbackInfo ci) {
        this.resizeGui();
    }
}
