package org.blackum.blackaddons.mixin.core;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.ping.ClientboundPongResponsePacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.zombie.Zombie;

import org.blackum.blackaddons.common.model.DungeonFloor;
import org.blackum.blackaddons.common.util.mc.LocationUtils;
import org.blackum.blackaddons.common.util.mc.ServerUtils;
import org.blackum.blackaddons.feature.dungeon.listener.DungeonListener;
import org.blackum.blackaddons.feature.dungeon.score.DungeonScore;
import org.blackum.blackaddons.feature.dungeon.solver.puzzle.tpmaze.TpMazeHandler;
import org.blackum.blackaddons.feature.dungeon.tracker.SoloClearTimer;
import org.blackum.blackaddons.feature.ping.PingFeature;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Inject(method = "handleEntityEvent", at = @At("HEAD"))
    private void onHandleEntityEvent(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        if (!LocationUtils.inDungeons())
            return;

        if (LocationUtils.inBoss())
            return;

        if (packet.getEventId() == 3) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null)
                return;

            Entity entity = packet.getEntity(mc.level);
            if (entity instanceof Zombie zombie && zombie.isBaby()) {
                DungeonFloor floor = LocationUtils.getCurrentFloor();
                if (floor != null) {
                    String floorName = floor.getDisplayName();
                    if (floorName.equals("F6") || floorName.equals("M6") || floorName.equals("F7") || floorName.equals("M7")) {
                        DungeonScore.onMimicKill();
                    }
                }
            } else if (entity instanceof Bat) {
                DungeonScore.onBatKill();
            }
        }
    }

    @Inject(method = "handleMovePlayer", at = @At("HEAD"))
    private void onHandlePlayerPositionPre(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
        TpMazeHandler.onServerTeleportPre(packet);
    }

    @Inject(method = "handleMovePlayer", at = @At("TAIL"))
    private void onHandlePlayerPositionPost(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
        TpMazeHandler.onServerTeleportPost();
    }

    @Inject(method = "handlePongResponse", at = @At("HEAD"), cancellable = true)
    private void onHandlePongResponse(ClientboundPongResponsePacket packet, CallbackInfo ci) {
        ServerUtils.onPongResponse(packet);
        if (PingFeature.onPongReceive(packet.time())) {
            ci.cancel();
        }
    }

    @Inject(method = "handleSetEntityData", at = @At("TAIL"))
    private void onHandleSetEntityData(ClientboundSetEntityDataPacket packet, CallbackInfo ci) {
        if (!LocationUtils.inDungeons())
            return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null)
            return;

        Entity entity = mc.level.getEntity(packet.id());
        if (entity instanceof ArmorStand armorStand) {
            Component customName = armorStand.getCustomName();
            if (customName != null) {
                String plain = ChatFormatting.stripFormatting(customName.getString());
                if ("Wither Key".equals(plain) || "Blood Key".equals(plain)) {
                    if (DungeonListener.lastDetectedKeyEntityId != packet.id()) {
                        DungeonListener.lastDetectedKeyEntityId = packet.id();
                        DungeonListener.keyTimerTicks = 200;
                        DungeonListener.keyTimerType = plain.replace(" Key", "");
                    }
                }
            }
        }
    }

    @Inject(method = "handleSetTime", at = @At("HEAD"))
    private void onHandleSetTime(ClientboundSetTimePacket packet, CallbackInfo ci) {
        ServerUtils.onSetTimePacket(packet);
        SoloClearTimer.onSetTimePacket();
    }
}
