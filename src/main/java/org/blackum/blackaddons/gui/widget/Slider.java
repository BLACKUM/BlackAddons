package org.blackum.blackaddons.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.blackum.blackaddons.gui.animation.Animation;
import org.blackum.blackaddons.gui.animation.Easing;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.render.RenderHelper;

import java.util.function.Consumer;

public class Slider extends Widget {
    private float value;
    private float minValue;
    private float maxValue;
    private Consumer<Float> onValueChange;
    private Consumer<Float> onRelease;
    private boolean dragging = false;
    private Animation hoverAnimation;

    public Slider(int x, int y, int width, float minValue, float maxValue, float initialValue,
            Consumer<Float> onValueChange) {
        this(x, y, width, Theme.SLIDER_HEIGHT + Theme.SLIDER_THUMB_SIZE, minValue, maxValue, initialValue,
                onValueChange);
    }

    public Slider(int x, int y, int width, int height, float minValue, float maxValue, float initialValue,
            Consumer<Float> onValueChange) {
        super(x, y, width, height);
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.value = initialValue;
        this.onValueChange = onValueChange;
        this.hoverAnimation = new Animation(0, 1, Theme.ANIM_HOVER, Easing::easeOut);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible)
            return;

        float hoverProgress = hoverAnimation.getValue();

        int trackY = y + (height - Theme.SLIDER_HEIGHT) / 2;
        int thumbX = x
                + (int) ((value - minValue) / (maxValue - minValue) * (width - Theme.SLIDER_THUMB_SIZE));
        int thumbY = y + (height - Theme.SLIDER_THUMB_SIZE) / 2;

        RenderHelper.renderSurface(graphics, x, trackY, width, Theme.SLIDER_HEIGHT,
                Theme.SLIDER_HEIGHT / 2, true);

        int filledWidth = thumbX - x + Theme.SLIDER_THUMB_SIZE / 2;
        if (filledWidth > 0) {
            graphics.fill(x + 2, trackY + 2, x + filledWidth, trackY + Theme.SLIDER_HEIGHT - 2,
                    Theme.withAlpha(Theme.ACCENT, 0.4f));
        }

        int thumbRadius = Theme.SLIDER_THUMB_SIZE / 2;
        RenderHelper.renderSurface(graphics, thumbX, thumbY,
                Theme.SLIDER_THUMB_SIZE, Theme.SLIDER_THUMB_SIZE, thumbRadius, dragging);

        if (hoverProgress > 0) {
            int highlightColor = Theme.withAlpha(Theme.GLASS_HIGHLIGHT, hoverProgress * 0.3f);
            RenderHelper.renderRoundedRect(graphics, thumbX, thumbY,
                    Theme.SLIDER_THUMB_SIZE, Theme.SLIDER_THUMB_SIZE, thumbRadius, highlightColor);
        }
    }

    @Override
    public void tick() {
        boolean thumbHovered = isMouseOverThumb();
        if (thumbHovered && hoverAnimation.getProgress() < 1
                && (!hoverAnimation.isRunning() || hoverAnimation.getValue() < 1)) {
            hoverAnimation = new Animation(hoverAnimation.getValue(), 1, Theme.ANIM_HOVER, Easing::easeOut);
            hoverAnimation.start();
        } else if (!thumbHovered && !dragging && hoverAnimation.getProgress() > 0
                && (!hoverAnimation.isRunning() || hoverAnimation.getValue() > 0)) {
            hoverAnimation = new Animation(hoverAnimation.getValue(), 0, Theme.ANIM_HOVER, Easing::easeOut);
            hoverAnimation.start();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, MouseButtonEvent event) {
        if (!enabled || !visible)
            return false;

        if (isMouseOver(mouseX, mouseY) && event.buttonInfo().button() == 0) {
            dragging = true;
            updateValue(mouseX);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, MouseButtonEvent event) {
        if (dragging && event.buttonInfo().button() == 0) {
            dragging = false;
            if (onRelease != null) {
                onRelease.accept(value);
            }
            return true;
        }
        return false;
    }

    public Slider onRelease(Consumer<Float> onRelease) {
        this.onRelease = onRelease;
        return this;
    }

    public Slider onValueChange(Consumer<Float> onValueChange) {
        this.onValueChange = onValueChange;
        return this;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, MouseButtonEvent event, double dragX, double dragY) {
        if (dragging) {
            updateValue(mouseX);
            return true;
        }
        return false;
    }

    private void updateValue(double mouseX) {
        float newValue = (float) ((mouseX - x) / width) * (maxValue - minValue) + minValue;
        newValue = Math.max(minValue, Math.min(maxValue, newValue));

        if (newValue != value) {
            value = newValue;
            if (onValueChange != null) {
                onValueChange.accept(value);
            }
        }
    }

    private boolean isMouseOverThumb() {
        return hovered;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = Math.max(minValue, Math.min(maxValue, value));
    }
}
