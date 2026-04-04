package org.blackum.blackaddons.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.blackum.blackaddons.gui.animation.Animation;
import org.blackum.blackaddons.gui.animation.Easing;
import net.minecraft.client.input.MouseButtonEvent;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.render.RenderHelper;

public class Button extends Widget {
    private String text;
    private final Runnable onClick;
    private Runnable onRightClick;
    private Animation hoverAnimation;
    private Animation pressAnimation;
    private boolean pressed = false;
    private Integer customTextColor = null;

    public Button(int x, int y, int width, String text, Runnable onClick) {
        this(x, y, width, Theme.BUTTON_HEIGHT, text, onClick);
    }

    public Button(int x, int y, int width, int height, String text, Runnable onClick) {
        super(x, y, width, height);
        this.text = text;
        this.onClick = onClick;
        this.hoverAnimation = new Animation(0, 1, Theme.ANIM_HOVER, Easing::easeOut);
        this.pressAnimation = new Animation(0, 1, Theme.ANIM_CLICK, Easing::easeInOut);
    }

    public Button setOnRightClick(Runnable onRightClick) {
        this.onRightClick = onRightClick;
        return this;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setTextColor(int color) {
        this.customTextColor = color;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible)
            return;

        float hoverProgress = hoverAnimation.getValue();
        float pressProgress = pressAnimation.getValue();

        RenderHelper.renderSurface(graphics, x, y, width, height,
                Theme.BORDER_RADIUS, pressed || pressProgress > 0);

        if (hoverProgress > 0) {
            int highlightColor = Theme.withAlpha(Theme.GLASS_HIGHLIGHT, hoverProgress * 0.4f);
            RenderHelper.renderRoundedRect(graphics, x, y, width, height, Theme.BORDER_RADIUS, highlightColor);
        }

        int textColor = customTextColor != null ? customTextColor
                : (enabled ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY);
        int textWidth = Minecraft.getInstance().font.width(text);
        int textX = x + (width - textWidth) / 2;
        int textY = y + (height - 8) / 2;
        graphics.text(Minecraft.getInstance().font, text, textX, textY, textColor);
    }

    @Override
    public void tick() {
        if (hovered && hoverAnimation.getProgress() < 1
                && (!hoverAnimation.isRunning() || hoverAnimation.getValue() < 1)) {
            hoverAnimation = new Animation(hoverAnimation.getValue(), 1, Theme.ANIM_HOVER, Easing::easeOut);
            hoverAnimation.start();
        } else if (!hovered && hoverAnimation.getProgress() > 0
                && (!hoverAnimation.isRunning() || hoverAnimation.getValue() > 0)) {
            hoverAnimation = new Animation(hoverAnimation.getValue(), 0, Theme.ANIM_HOVER, Easing::easeOut);
            hoverAnimation.start();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, MouseButtonEvent event) {
        if (!enabled || !visible)
            return false;

        int button = event.buttonInfo().button();
        if (isMouseOver(mouseX, mouseY) && (button == 0 || button == 1)) {
            pressed = true;
            pressAnimation = new Animation(0, 1, Theme.ANIM_CLICK, Easing::easeInOut);
            pressAnimation.start();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, MouseButtonEvent event) {
        if (!enabled || !visible)
            return false;

        int button = event.buttonInfo().button();
        if (pressed && (button == 0 || button == 1)) {
            pressed = false;
            pressAnimation = new Animation(pressAnimation.getValue(), 0, Theme.ANIM_CLICK, Easing::easeInOut);
            pressAnimation.start();

            if (isMouseOver(mouseX, mouseY)) {
                if (button == 0 && onClick != null) {
                    onClick.run();
                } else if (button == 1 && onRightClick != null) {
                    onRightClick.run();
                }
            }
            return true;
        }
        return false;
    }
}
