package org.blackum.blackaddons.mixin.gui;

import net.minecraft.client.gui.components.toasts.SystemToast;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SystemToast.class)
public interface SystemToastAccessor {
    @Accessor("id")
    SystemToast.SystemToastId getId();

    @Accessor("title")
    net.minecraft.network.chat.Component getTitle();
}
