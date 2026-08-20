package org.blackum.blackaddons.common.util.mc;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.ping.ClientboundPongResponsePacket;
import net.minecraft.network.protocol.ping.ServerboundPingRequestPacket;

import java.util.ArrayList;
import java.util.List;

public class ServerUtils {
    private static final float DEFAULT_TPS = 20.0f;
    private static final float MAX_TPS = 20.0f;
    private static final float MIN_TPS = 0.0f;
    private static final float TPS_CALC_BASE = 20000.0f;
    private static final int PING_INTERVAL_TICKS = 80;
    private static final long PING_TIMEOUT_NANOS = 10000000000L;
    private static final int MAX_PING_SAMPLES = 10;

    private static float tps = DEFAULT_TPS;
    private static long currentPing = 0L;
    private static long averagePing = 0L;
    private static long lastTimePacket = 0L;
    private static long pingStartTime = 0L;
    private static boolean isPinging = false;
    private static int tickCounter = 0;
    private static long lastPacketTime = 0L;
    private static final List<Long> pingHistory = new ArrayList<>();

    public static void reset() {
        tps = DEFAULT_TPS;
        currentPing = 0L;
        averagePing = 0L;
        lastTimePacket = 0L;
        isPinging = false;
        tickCounter = 0;
        lastPacketTime = System.currentTimeMillis();
        pingHistory.clear();
    }

    public static void tick() {
        if (isPinging && System.nanoTime() - pingStartTime > PING_TIMEOUT_NANOS) {
            isPinging = false;
        }

        tickCounter++;
        if (tickCounter >= PING_INTERVAL_TICKS) {
            tickCounter = 0;
            sendPingRequest();
        }
    }

    public static void onSetTimePacket(ClientboundSetTimePacket packet) {
        long now = System.currentTimeMillis();
        if (lastTimePacket != 0L) {
            long diff = now - lastTimePacket;
            if (diff > 0L) {
                tps = Math.min(MAX_TPS, Math.max(MIN_TPS, TPS_CALC_BASE / diff));
            }
        }
        lastTimePacket = now;
        lastPacketTime = now;
    }

    public static void onPongResponse(ClientboundPongResponsePacket packet) {
        currentPing = Math.max(0L, System.currentTimeMillis() - packet.time());
        isPinging = false;
        pingHistory.add(currentPing);
        while (pingHistory.size() > MAX_PING_SAMPLES) {
            pingHistory.remove(0);
        }

        long total = 0L;
        for (long sample : pingHistory) {
            total += sample;
        }
        averagePing = pingHistory.isEmpty() ? currentPing : total / pingHistory.size();
    }

    public static void onPacket(Packet<?> packet) {
        lastPacketTime = System.currentTimeMillis();
    }

    private static void sendPingRequest() {
        if (isPinging) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) {
            return;
        }

        isPinging = true;
        pingStartTime = System.nanoTime();
        mc.getConnection().send(new ServerboundPingRequestPacket(System.currentTimeMillis()));
    }

    public static float getTps() {
        return tps;
    }

    public static long getCurrentPing() {
        return currentPing;
    }

    public static long getAveragePing() {
        return averagePing;
    }

    public static long getLastTimePacket() {
        return lastTimePacket;
    }

    public static long getLastPacketTime() {
        return lastPacketTime;
    }
}
