package org.blackum.blackaddons.mixin.gui;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.feature.modhider.ComponentUtils;

import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static org.blackum.blackaddons.mixin.gui.AbstractSignEditScreenMixin.Helper.showServerAttemptedReadingModsNotification;

@Mixin(AnvilMenu.class)
public class AnvilMenuMixin {
    @WrapOperation(method = "createResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Component;getString()Ljava/lang/String;"))
    public String getString(Component instance, Operation<String> original) {
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
