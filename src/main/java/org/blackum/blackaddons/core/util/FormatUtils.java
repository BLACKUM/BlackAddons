package org.blackum.blackaddons.core.util;

import java.util.Locale;

public class FormatUtils {

    public static String formatMs(int ms) {
        if (ms == 0)
            return "-";
        int seconds = ms / 1000;
        int millis = ms % 1000;
        int m = seconds / 60;
        int s = seconds % 60;
        return String.format(Locale.ROOT, "%d:%02d.%03d", m, s, millis);
    }

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

    public static String formatNumber(double value) {
        if (value >= 1_000_000_000) {
            return String.format(Locale.ROOT, "%.1fB", value / 1_000_000_000);
        } else if (value >= 1_000_000) {
            return String.format(Locale.ROOT, "%.1fM", value / 1_000_000);
        } else if (value >= 1_000) {
            return String.format(Locale.ROOT, "%.0fk", value / 1_000);
        } else {
            return String.format(Locale.ROOT, "%,.0f", value);
        }
    }

    public static String formatTime(long seconds) {
        if (seconds < 0)
            seconds = 0;
        if (seconds < 60)
            return seconds + "s";
        if (seconds < 3600)
            return (seconds / 60) + "m " + (seconds % 60) + "s";
        return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m " + (seconds % 60) + "s";
    }

    public static String formatColor(String text) {
        if (text == null)
            return null;
        return text.replace('&', '§');
    }
}
