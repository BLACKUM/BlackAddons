package org.blackum.blackaddons.mixin.gui;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.feature.modhider.ComponentUtils;

import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static org.blackum.blackaddons.mixin.gui.AbstractSignEditScreenMixin.Helper.showServerAttemptedReadingModsNotification;

@Mixin(AnvilScreen.class)
public class AnvilScreenMixin {
    @WrapOperation(method = "slotChanged", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Component;getString()Ljava/lang/String;"))
    public String slotChanged$getString(Component instance, Operation<String> original) {
        if (ConfigManager.data.hideMods()) {
            String str = ComponentUtils.getString(instance);
            if (!str.equals(instance.getString())) {
                showServerAttemptedReadingModsNotification();
            }
            return str;
        }
        return original.call(instance);
    }

    @WrapOperation(method = "onNameChanged", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Component;getString()Ljava/lang/String;"))
    public String onNameChanged$getString(Component instance, Operation<String> original) {
        if (ConfigManager.data.hideMods()) {
            String str = ComponentUtils.getString(instance);
            if (!str.equals(instance.getString())) {
                showServerAttemptedReadingModsNotification();
            }
            return str;
        }
        return original.call(instance);
    }
}
