package org.blackum.blackaddons.mixin.core;

import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.feature.modhider.SpoofMode;

import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.client.Minecraft;
import org.blackum.blackaddons.feature.chat.PacketLogger;
import org.blackum.blackaddons.gui.notification.NotificationManager;
import org.blackum.blackaddons.gui.notification.NotificationType;
import org.blackum.blackaddons.core.util.PayloadHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.HashSet;

@Mixin(Connection.class)
public abstract class ConnectionMixin {

    @Shadow
    public abstract void sendPacket(Packet<?> packet, ChannelFutureListener channelFutureListener, boolean bl);

    @Unique
    private static final ThreadLocal<Boolean> BLACKADDONS$SENDING = ThreadLocal.withInitial(() -> false);

    @Inject(method = "sendPacket", at = @At("HEAD"), cancellable = true)
    public void sendPacket(Packet<?> packet, ChannelFutureListener channelFutureListener, boolean bl, CallbackInfo ci) {
        if (BLACKADDONS$SENDING.get()) {
            return;
        }

        if (((Connection) (Object) this).isMemoryConnection()) {
            return;
        }

        if (packet instanceof ServerboundCustomPayloadPacket(CustomPacketPayload payload)) {
            String id = payload.type().id().toString();
            if (id.equals("minecraft:register") || id.equals("minecraft:unregister")) {
                if (ConfigManager.data.modHiderSpoofMode == SpoofMode.CUSTOM
                        && ConfigManager.data.modHiderDisableCustomPayloads) {
                    CustomPacketPayload newPayload = PayloadHelper.createRegisterPayload(
                            payload,
                            new HashSet<>(ConfigManager.data.modHiderAllowedCustomPayloadChannels));
                    if (newPayload != null) {
                        BLACKADDONS$SENDING.set(true);
                        this.sendPacket(new ServerboundCustomPayloadPacket(newPayload),
                                channelFutureListener, bl);
                        BLACKADDONS$SENDING.set(false);
                        ci.cancel();
                        return;
                    }
                }
            }

            if (!(payload instanceof DiscardedPayload) && !(payload instanceof BrandPayload)) {
                if (ConfigManager.data.modHiderSpoofMode == SpoofMode.OFF) {
                    return;
                }

                if (ConfigManager.data.modHiderSpoofMode == SpoofMode.VANILLA) {
                    PacketLogger.logBlockedPacket("ModHider (Vanilla)", payload);
                    Minecraft.getInstance().execute(() -> {
                        NotificationManager.addNotification(
                                "Mod Hider",
                                "Blocked payload: " + payload.type().id(),
                                NotificationType.INFO);
                    });
                    ci.cancel();
                    return;
                }

                if (ConfigManager.data.modHiderSpoofMode == SpoofMode.MODDED) {
                    return;
                }

                if (ConfigManager.data.modHiderSpoofMode == SpoofMode.CUSTOM &&
                        ConfigManager.data.modHiderDisableCustomPayloads) {
                    for (String channel : ConfigManager.data.modHiderAllowedCustomPayloadChannels) {
                        if (id.toLowerCase().startsWith(channel.toLowerCase())) {
                            return;
                        }
                    }
                    PacketLogger.logBlockedPacket("ModHider (Custom)", payload);
                    Minecraft.getInstance().execute(() -> {
                        NotificationManager.addNotification(
                                "Mod Hider",
                                "Blocked payload: " + payload.type().id(),
                                NotificationType.INFO);
                    });
                    ci.cancel();
                }
            }
        }
    }
}
