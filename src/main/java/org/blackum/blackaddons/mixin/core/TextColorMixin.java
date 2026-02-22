package org.blackum.blackaddons.mixin.core;

import net.minecraft.network.chat.TextColor;
import org.blackum.blackaddons.core.util.AnimatedTextColorAccessor;
import org.blackum.blackaddons.feature.chat.ChatUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(TextColor.class)
public abstract class TextColorMixin implements AnimatedTextColorAccessor {
    @Shadow
    @Final
    private int value;

    @Unique
    private boolean ba$isAnimated = false;
    @Unique
    private List<ChatUtils.ColorStop> ba$stops;
    @Unique
    private float ba$speed = 1.0f;
    @Unique
    private float ba$offset = 0.0f;

    @Inject(method = "getValue", at = @At("HEAD"), cancellable = true)
    private void onGetValue(CallbackInfoReturnable<Integer> cir) {
        if (ba$isAnimated && ba$stops != null && !ba$stops.isEmpty()) {
            cir.setReturnValue(ba$calculateAnimatedColor());
        }
    }

    @Overwrite
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof TextColor that))
            return false;
        if (this.value != that.getValue())
            return false;

        if (o instanceof AnimatedTextColorAccessor thatAcc) {
            return this.ba$isAnimated == ((TextColorMixin) thatAcc).ba$isAnimated &&
                    Float.compare(this.ba$offset, ((TextColorMixin) thatAcc).ba$offset) == 0;
        }
        return !ba$isAnimated;
    }

    @Overwrite
    public int hashCode() {
        int result = Integer.hashCode(value);
        if (ba$isAnimated) {
            result = 31 * result + Boolean.hashCode(ba$isAnimated);
            result = 31 * result + Float.hashCode(ba$offset);
        }
        return result;
    }

    @Unique
    private int ba$calculateAnimatedColor() {
        if (ba$stops.size() == 1) {
            return ba$stops.get(0).rgb();
        }

        double time = (System.currentTimeMillis() / 1000.0) * ba$speed * 0.5;
        double ratio = (time + ba$offset) % 2.0;
        if (ratio < 0)
            ratio += 2.0;

        float finalRatio = (float) (ratio > 1.0 ? 2.0 - ratio : ratio);

        ChatUtils.ColorStop startStop = ba$stops.get(0);
        ChatUtils.ColorStop endStop = ba$stops.get(ba$stops.size() - 1);

        for (int i = 0; i < ba$stops.size() - 1; i++) {
            if (finalRatio >= ba$stops.get(i).fraction() && finalRatio <= ba$stops.get(i + 1).fraction()) {
                startStop = ba$stops.get(i);
                endStop = ba$stops.get(i + 1);
                break;
            }
        }

        if (startStop.fraction() == endStop.fraction()) {
            return startStop.rgb();
        }

        float localRatio = (finalRatio - startStop.fraction()) / (endStop.fraction() - startStop.fraction());

        int sr = (startStop.rgb() >> 16) & 0xFF;
        int sg = (startStop.rgb() >> 8) & 0xFF;
        int sb = startStop.rgb() & 0xFF;

        int er = (endStop.rgb() >> 16) & 0xFF;
        int eg = (endStop.rgb() >> 8) & 0xFF;
        int eb = endStop.rgb() & 0xFF;

        int r = Math.round(sr + localRatio * (er - sr));
        int g = Math.round(sg + localRatio * (eg - sg));
        int b = Math.round(sb + localRatio * (eb - sb));

        return (r << 16) | (g << 8) | b;
    }

    @Override
    public void ba$setAnimated(boolean animated) {
        this.ba$isAnimated = animated;
    }

    @Override
    public void ba$setStops(List<ChatUtils.ColorStop> stops) {
        this.ba$stops = stops;
    }

    @Override
    public void ba$setSpeed(float speed) {
        this.ba$speed = speed;
    }

    @Override
    public void ba$setOffset(float offset) {
        this.ba$offset = offset;
    }

    @Override
    public boolean ba$isAnimated() {
        return this.ba$isAnimated;
    }
}
