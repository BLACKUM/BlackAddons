package org.blackum.blackaddons.mixin.gui;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.core.manager.CustomNameManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {

    @Unique
    private boolean blackaddons$modifyingTitle = false;

    @Unique
    private boolean blackaddons$modifyingSubtitle = false;

    @Unique
    private boolean blackaddons$modifyingOverlay = false;

    @Inject(method = "setTitle", at = @At("HEAD"), cancellable = true)
    private void onSetTitle(Component component, CallbackInfo ci) {
        if (this.blackaddons$modifyingTitle || component == null)
            return;
        Component newComp = CustomNameManager.getInstance().replaceNames(component);
        if (newComp != component) {
            this.blackaddons$modifyingTitle = true;
            ((Gui) (Object) this).setTitle(newComp);
            this.blackaddons$modifyingTitle = false;
            ci.cancel();
        }
    }

    @Inject(method = "setSubtitle", at = @At("HEAD"), cancellable = true)
    private void onSetSubtitle(Component component, CallbackInfo ci) {
        if (this.blackaddons$modifyingSubtitle || component == null)
            return;
        Component newComp = CustomNameManager.getInstance().replaceNames(component);
        if (newComp != component) {
            this.blackaddons$modifyingSubtitle = true;
            ((Gui) (Object) this).setSubtitle(newComp);
            this.blackaddons$modifyingSubtitle = false;
            ci.cancel();
        }
    }

    @Inject(method = "setOverlayMessage", at = @At("HEAD"), cancellable = true)
    private void onSetActionBar(Component component, boolean animate, CallbackInfo ci) {
        if (this.blackaddons$modifyingOverlay || component == null)
            return;
        Component newComp = CustomNameManager.getInstance().replaceNames(component);
        if (newComp != component) {
            this.blackaddons$modifyingOverlay = true;
            ((Gui) (Object) this).setOverlayMessage(newComp, animate);
            this.blackaddons$modifyingOverlay = false;
            ci.cancel();
        }
    }

    @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
    private void onRenderEffects(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (ConfigManager.data.hideStatusEffects) {
            ci.cancel();
        }
    }
}
