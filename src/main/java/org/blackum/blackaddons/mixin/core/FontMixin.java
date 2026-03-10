package org.blackum.blackaddons.mixin.core;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.blackum.blackaddons.core.manager.CustomNameManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = Font.class, priority = 1100)
public class FontMixin {

    @ModifyVariable(method = "prepareText(Ljava/lang/String;FFIZI)Lnet/minecraft/client/gui/Font$PreparedText;",
            at = @At("HEAD"), argsOnly = true, index = 1)
    private String onDrawString(String text) {
        return CustomNameManager.getInstance().replaceInString(text);
    }

    @ModifyVariable(method = "prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZI)Lnet/minecraft/client/gui/Font$PreparedText;",
            at = @At("HEAD"), argsOnly = true, index = 1)
    private FormattedCharSequence onDrawSequence(FormattedCharSequence text) {
        return CustomNameManager.getInstance().replaceInSequence(text);
    }

    @ModifyVariable(method = "width(Ljava/lang/String;)I", at = @At("HEAD"), argsOnly = true, index = 1)
    private String onWidthString(String text) {
        return CustomNameManager.getInstance().replaceInString(text);
    }

    @ModifyVariable(method = "width(Lnet/minecraft/network/chat/FormattedText;)I", at = @At("HEAD"), argsOnly = true, index = 1)
    private FormattedText onWidthComponent(FormattedText text) {
        if (text instanceof Component c) return CustomNameManager.getInstance().replaceNames(c);
        return text;
    }

    @ModifyVariable(method = "width(Lnet/minecraft/util/FormattedCharSequence;)I", at = @At("HEAD"), argsOnly = true, index = 1)
    private FormattedCharSequence onWidthSequence(FormattedCharSequence text) {
        return CustomNameManager.getInstance().replaceInSequence(text);
    }
}
