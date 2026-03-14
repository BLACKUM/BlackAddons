package org.blackum.blackaddons.core.util;

import org.blackum.blackaddons.feature.chat.ChatUtils;

import java.util.concurrent.ThreadLocalRandom;

public class TimeUtils {
    public static String formatRelativeTime(long timestamp) {
        if (timestamp == 0)
            return "Unknown";
        long now = System.currentTimeMillis() / 1000;
        long diff = now - timestamp;

        if (diff < 60)
            return diff + "s";
        if (diff < 3600)
            return (diff / 60) + "m";
        if (diff < 86400)
            return (diff / 3600) + "h";
        return (diff / 86400) + "d";
    }

    public static String formatMs(int ms) {
        if (ms == 0)
            return "-";
        int seconds = ms / 1000;
        int millis = ms % 1000;
        int m = seconds / 60;
        int s = seconds % 60;
        return String.format("%d:%02d.%03d", m, s, millis);
    }

    private long finishTick = -1;

    public void startRandomTimer(long currentTick, float minMs, float maxMs) {
        float randomMs = ThreadLocalRandom.current().nextFloat(minMs, maxMs);
        this.finishTick = currentTick + Math.round(randomMs / 50.0);
    }

    public boolean isFinished(long currentTick) {
        if (finishTick == -1) return false;
        long ticksLeft = finishTick - currentTick;
        long msLeft = ticksLeft * 50;
        if (ticksLeft > 0) {
            ChatUtils.send_debug("Time remaining: " + msLeft + "ms (" + ticksLeft + " ticks)");
        }

        if (currentTick >= finishTick) {
//            ChatUtils.send_debug("Timer Finished!");
            return true;
        }

        return false;
    }

    public void reset() {
        this.finishTick = -1;
    }
}
