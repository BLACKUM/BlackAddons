package org.blackum.blackaddons.mixin.gui;

import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.gui.components.toasts.SystemToast;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ToastManager.class)
public class ToastManagerMixin {
    @Inject(method = "addToast", at = @At("HEAD"), cancellable = true)
    private void onAdd(Toast toast, CallbackInfo ci) {
        if (ConfigManager.data.disableUnsecureChatToast && toast instanceof SystemToast systemToast) {
            SystemToast.SystemToastId id = ((SystemToastAccessor) systemToast).getId();
            if (id == SystemToast.SystemToastId.UNSECURE_SERVER_WARNING) {
                ci.cancel();
                return;
            }

            net.minecraft.network.chat.Component title = ((SystemToastAccessor) systemToast).getTitle();
            if (title != null) {
                String text = title.getString();
                if (text.contains("Chat messages can't be verified")
                        || text.equals("multiplayer.unsecureserver.toast.title")) {
                    ci.cancel();
                }
            }
        }
    }
}
