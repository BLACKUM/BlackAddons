package org.blackum.blackaddons.mixin.core;

import net.minecraft.network.chat.TextColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(TextColor.class)
public interface TextColorAccessor {
    @Invoker("<init>")
    static TextColor ba$create(int value) {
        throw new UnsupportedOperationException();
    }
}
