package org.blackum.blackaddons.feature.chat;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.blackum.blackaddons.core.util.AnimatedTextColorAccessor;
import org.blackum.blackaddons.mixin.core.TextColorAccessor;
import static org.blackum.blackaddons.core.util.MinecraftInstance.mc;

public class ChatUtils {

    public static MutableComponent BuildGradient(String text, int startRgb, int endRgb) {
        MutableComponent result = Component.empty();
        int length = text.length();

        if (length <= 1) {
            return Component.literal(text).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(startRgb)));
        }

        int sr = (startRgb >> 16) & 0xFF;
        int sg = (startRgb >> 8) & 0xFF;
        int sb = startRgb & 0xFF;

        int er = (endRgb >> 16) & 0xFF;
        int eg = (endRgb >> 8) & 0xFF;
        int eb = endRgb & 0xFF;

        for (int i = 0; i < text.length(); ++i) {
            float ratio = (float) i / (length - 1);

            int r = Math.round(sr + ratio * (er - sr));
            int g = Math.round(sg + ratio * (eg - sg));
            int b = Math.round(sb + ratio * (eb - sb));

            int rgb = (r << 16) | (g << 8) | b;

            MutableComponent charText = Component.literal(String.valueOf(text.charAt(i)))
                    .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb)));

            result.append(charText);
        }

        return result;
    }

    public record ColorStop(int rgb, float fraction) {
    }

    public static MutableComponent BuildAnimatedMultiGradient(String text, java.util.List<ColorStop> stops,
            float speed) {
        if (stops == null || stops.isEmpty()) {
            return Component.literal(text);
        }

        MutableComponent result = Component.empty();
        int length = text.length();

        for (int i = 0; i < length; ++i) {
            float charOffset = (float) i / Math.max(1, length - 1);
            TextColor color = org.blackum.blackaddons.mixin.core.TextColorAccessor.ba$create(stops.get(0).rgb());

            if ((Object) color instanceof org.blackum.blackaddons.core.util.AnimatedTextColorAccessor accessor) {
                accessor.ba$setAnimated(true);
                accessor.ba$setStops(stops);
                accessor.ba$setSpeed(speed);
                accessor.ba$setOffset(charOffset);
            }

            MutableComponent charText = Component.literal(String.valueOf(text.charAt(i)))
                    .setStyle(Style.EMPTY.withColor(color));

            result.append(charText);
        }

        return result;
    }

    private static final float CHROMA_HUE_SPREAD = 0.3f;

    public static MutableComponent BuildChroma(String text, float speed) {
        MutableComponent result = Component.empty();
        int length = text.length();

        for (int i = 0; i < length; ++i) {
            float charOffset = length > 1 ? ((float) (length - 1 - i) / (length - 1)) * CHROMA_HUE_SPREAD : 0.0f;
            TextColor color = TextColorAccessor.ba$create(0xFFFFFF);

            if ((Object) color instanceof AnimatedTextColorAccessor accessor) {
                accessor.ba$setChroma(true);
                accessor.ba$setSpeed(speed);
                accessor.ba$setOffset(charOffset);
            }

            result.append(Component.literal(String.valueOf(text.charAt(i)))
                    .setStyle(Style.EMPTY.withColor(color)));
        }

        return result;
    }

    public static MutableComponent BuildMultiGradient(String text, java.util.List<ColorStop> stops) {
        if (stops == null || stops.isEmpty()) {
            return Component.literal(text);
        }

        if (stops.size() == 1) {
            return Component.literal(text).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(stops.get(0).rgb())));
        }

        MutableComponent result = Component.empty();
        int length = text.length();

        if (length <= 1) {
            return Component.literal(text).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(stops.get(0).rgb())));
        }

        for (int i = 0; i < length; ++i) {
            float ratio = (float) i / (length - 1);

            ColorStop startStop = stops.get(0);
            ColorStop endStop = stops.get(stops.size() - 1);

            for (int j = 0; j < stops.size() - 1; j++) {
                if (ratio >= stops.get(j).fraction() && ratio <= stops.get(j + 1).fraction()) {
                    startStop = stops.get(j);
                    endStop = stops.get(j + 1);
                    break;
                }
            }

            int rgb;
            if (startStop.fraction() == endStop.fraction()) {
                rgb = startStop.rgb();
            } else {
                float localRatio = (ratio - startStop.fraction()) / (endStop.fraction() - startStop.fraction());

                int sr = (startStop.rgb() >> 16) & 0xFF;
                int sg = (startStop.rgb() >> 8) & 0xFF;
                int sb = startStop.rgb() & 0xFF;

                int er = (endStop.rgb() >> 16) & 0xFF;
                int eg = (endStop.rgb() >> 8) & 0xFF;
                int eb = endStop.rgb() & 0xFF;

                int r = Math.round(sr + localRatio * (er - sr));
                int g = Math.round(sg + localRatio * (eg - sg));
                int b = Math.round(sb + localRatio * (eb - sb));

                rgb = (r << 16) | (g << 8) | b;
            }

            MutableComponent charText = Component.literal(String.valueOf(text.charAt(i)))
                    .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb)));

            result.append(charText);
        }

        return result;
    }

    private static final MutableComponent PREFIX = Component.empty()
            .append(ChatFormatting.BLACK + "[")
            .append(BuildAnimatedMultiGradient("BlackAddons", java.util.List.of(
                    new ColorStop(0x332640, 0.0f),
                    new ColorStop(0x623d94, 1.0f)), 0.8f))
            .append(ChatFormatting.BLACK + "] ");

    public static MutableComponent getPrefix() {
        return PREFIX.copy();
    }

    public static MutableComponent getMessage(String text) {
        if (text == null)
            text = "";
        return getPrefix().append(Component.literal(text).withStyle(ChatFormatting.GRAY));
    }

    public static MutableComponent error(String text) {
        if (text == null)
            text = "Unknown error";
        return getPrefix().append(Component.literal(text).withStyle(ChatFormatting.RED));
    }

    public static MutableComponent success(String text) {
        if (text == null)
            text = "";
        return getPrefix().append(Component.literal(text).withStyle(ChatFormatting.GREEN));
    }

    public static void send_debug(String txt) {
        mc.gui.getChat().addMessage(getMessage(txt));
    }
}
