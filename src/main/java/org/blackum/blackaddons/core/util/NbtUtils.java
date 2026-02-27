package org.blackum.blackaddons.core.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtAccounter;
import java.io.ByteArrayInputStream;
import java.util.Base64;

public class NbtUtils {
    public static CompoundTag decodeBase64Compressed(String base64) {
        if (base64 == null || base64.isEmpty())
            return new CompoundTag();
        try {
            byte[] data = Base64.getDecoder().decode(base64);
            return NbtIo.readCompressed(new ByteArrayInputStream(data), NbtAccounter.unlimitedHeap());
        } catch (Exception e) {
            return new CompoundTag();
        }
    }
}
