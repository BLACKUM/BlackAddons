package org.blackum.blackaddons.gui.render;

import java.awt.Color;

public class ColorUtils {

    public static String toHex(int color) {
        return String.format("#%06X", (color & 0xFFFFFF));
    }

    public static String toRGB(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        return String.format("%d, %d, %d", r, g, b);
    }

    public static String toRGBA(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (color >> 24) & 0xFF;
        if (a == 0 && color != 0)
            a = 255;
        return String.format("%d, %d, %d, %d", r, g, b, a);
    }

    public static String toCMYK(int color) {
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        float k = 1.0f - Math.max(r, Math.max(g, b));
        if (k == 1.0f) {
            return "0%, 0%, 0%, 100%";
        }
        float c = (1.0f - r - k) / (1.0f - k);
        float m = (1.0f - g - k) / (1.0f - k);
        float y = (1.0f - b - k) / (1.0f - k);

        return String.format("%d%%, %d%%, %d%%, %d%%",
                (int) (c * 100), (int) (m * 100), (int) (y * 100), (int) (k * 100));
    }

    public static String toHSV(int color) {
        float[] hsv = new float[3];
        Color.RGBtoHSB((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, hsv);
        return String.format("%d°, %d%%, %d%%",
                (int) (hsv[0] * 360), (int) (hsv[1] * 100), (int) (hsv[2] * 100));
    }

    public static String toHSL(int color) {
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float h, s, l = (max + min) / 2f;

        if (max == min) {
            h = s = 0;
        } else {
            float d = max - min;
            s = l > 0.5f ? d / (2f - max - min) : d / (max + min);
            if (max == r) {
                h = (g - b) / d + (g < b ? 6f : 0f);
            } else if (max == g) {
                h = (b - r) / d + 2f;
            } else {
                h = (r - g) / d + 4f;
            }
            h /= 6f;
        }

        return String.format("%d°, %d%%, %d%%",
                (int) (h * 360), (int) (s * 100), (int) (l * 100));
    }

    public static int hsvToRgb(float h, float s, float v) {
        return Color.HSBtoRGB(h, s, v);
    }

    public static float[] toHSB(int color) {
        float[] hsb = new float[3];
        Color.RGBtoHSB((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, hsb);
        return hsb;
    }

    public static Integer parseHex(String hex) {
        try {
            if (hex.startsWith("#"))
                hex = hex.substring(1);
            return (int) Long.parseLong(hex, 16);
        } catch (Exception e) {
            return null;
        }
    }

    public static Integer parseRGBA(String rgba) {
        try {
            String[] parts = rgba.split(",");
            if (parts.length < 3)
                return null;
            int r = Integer.parseInt(parts[0].trim());
            int g = Integer.parseInt(parts[1].trim());
            int b = Integer.parseInt(parts[2].trim());
            int a = parts.length > 3 ? Integer.parseInt(parts[3].trim()) : 255;
            return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
        } catch (Exception e) {
            return null;
        }
    }
    public static int getRainbow(int speed, float saturation, float brightness) {
        float hue = (System.currentTimeMillis() % (speed * 1000)) / (float) (speed * 1000);
        return Color.HSBtoRGB(hue, saturation, brightness);
    }
}
