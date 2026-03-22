package org.blackum.blackaddons.mixin.skyhanni;

import org.blackum.blackaddons.core.config.ConfigManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "at.hannibal2.skyhanni.features.chat.CurrentChatDisplay", remap = false)
public class CurrentChatDisplayMixin {

    @Inject(method = "drawDisplay", at = @At("HEAD"), cancellable = true)
    private void onDrawDisplay(CallbackInfoReturnable<String> cir) {
        if (ConfigManager.data.ircChatMode) {
            cir.setReturnValue("§aChat: §6IRC");
        }
    }
}
