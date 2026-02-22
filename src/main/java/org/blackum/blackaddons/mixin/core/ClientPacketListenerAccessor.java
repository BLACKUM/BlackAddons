package org.blackum.blackaddons.mixin.core;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.UUID;

@Mixin(ClientPacketListener.class)
public interface ClientPacketListenerAccessor {
    @Accessor("playerInfoMap")
    Map<UUID, PlayerInfo> getPlayerInfoMap();
}
