package org.blackum.blackaddons.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import org.blackum.blackaddons.gui.animation.Animation;
import org.blackum.blackaddons.gui.animation.Easing;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.render.RenderHelper;
import org.blackum.blackaddons.gui.screen.BaseScreen;

import java.util.function.Consumer;

public class RadioButton extends Widget {

    private boolean selected;
    private String label;
    private String groupName;
    private Consumer<Boolean> onSelect;
    private Animation selectAnimation;
    private Animation hoverAnimation;

    public RadioButton(int x, int y, String label, String groupName, boolean initialState,
            Consumer<Boolean> onSelect) {
        this(x, y, Theme.RADIO_SIZE, label, groupName, initialState, onSelect);
    }

    public RadioButton(int x, int y, int height, String label, String groupName, boolean initialState,
            Consumer<Boolean> onSelect) {
        super(x, y, height + (label.isEmpty() ? 0 : Minecraft.getInstance().font.width(label) + 8),
                height);
        this.label = label;
        this.groupName = groupName;
        this.selected = initialState;
        this.onSelect = onSelect;
        this.selectAnimation = new Animation(initialState ? 1 : 0, initialState ? 1 : 0, Theme.ANIM_CLICK,
                Easing::easeOutBack);
        this.hoverAnimation = new Animation(0, 1, Theme.ANIM_HOVER, Easing::easeOut);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible)
            return;

        float selectProgress = selectAnimation.getValue();
        float hoverProgress = hoverAnimation.getValue();

        int radioRadius = height / 2;
        RenderHelper.renderSurface(graphics, x, y, height, height,
                radioRadius, selected);

        if (selectProgress > 0) {
            int innerSize = (int) ((height - 10) * selectProgress);
            int innerX = x + (height - innerSize) / 2;
            int innerY = y + (height - innerSize) / 2;
            int innerColor = Theme.withAlpha(Theme.ACCENT, selectProgress * 0.9f);

            graphics.fill(innerX, innerY, innerX + innerSize, innerY + innerSize, innerColor);
        }

        if (hoverProgress > 0) {
            int highlightColor = Theme.withAlpha(Theme.GLASS_HIGHLIGHT, hoverProgress * 0.3f);
            RenderHelper.renderRoundedRect(graphics, x, y, height, height,
                    radioRadius, highlightColor);
        }

        if (!label.isEmpty()) {
            int labelX = x + height + 8;
            int labelY = y + (height - 8) / 2;
            int labelColor = enabled ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY;
            graphics.text(Minecraft.getInstance().font, label, labelX, labelY, labelColor);
        }
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

        if (isMouseOver(mouseX, mouseY) && !selected && event.button() == 0) {
            if (onSelect != null) {
                select();
                return true;
            }
        }
        return false;
    }

    private void select() {
        if (!selected) {
            deselectGroup(groupName, this);

            selected = true;
            selectAnimation = new Animation(selectAnimation.getValue(), 1, Theme.ANIM_CLICK,
                    Easing::easeOutBack);
            selectAnimation.start();

            if (onSelect != null) {
                onSelect.accept(true);
            }
        }
    }

    private void deselect() {
        if (selected) {
            selected = false;
            selectAnimation = new Animation(selectAnimation.getValue(), 0, Theme.ANIM_CLICK,
                    Easing::easeOutBack);
            selectAnimation.start();

            if (onSelect != null) {
                onSelect.accept(false);
            }
        }
    }

    private void deselectGroup(String groupName, RadioButton except) {
        if (Minecraft.getInstance().screen instanceof BaseScreen base) {
            for (Widget w : base.getWidgets()) {
                if (w instanceof RadioButton rb && rb.groupName.equals(groupName) && rb != except) {
                    rb.deselect();
                }
            }
        }
    }

    public boolean isSelected() {
        return selected;
    }
}
