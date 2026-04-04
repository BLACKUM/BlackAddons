package org.blackum.blackaddons.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import org.blackum.blackaddons.gui.animation.Animation;
import org.blackum.blackaddons.gui.animation.Easing;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.render.RenderHelper;

import java.util.function.Consumer;

public class ToggleSwitch extends Widget {

    private static final int SWITCH_WIDTH = 40;
    private static final int SWITCH_HEIGHT = 20;
    private static final int THUMB_SIZE = 16;
    private static final int EXPAND_ICON_SIZE = 8;

    private String label;
    private int labelColor = Theme.TEXT_PRIMARY;
    private String description;
    private boolean value;
    private boolean expanded = false;
    private Consumer<Boolean> onChange;
    private Runnable onExpandChange;

    private Animation toggleAnimation;
    private Animation hoverAnimation;
    private Animation expandAnimation;

    private int labelWidth;
    private int descriptionHeight = 0;

    public ToggleSwitch(int x, int y, int width, String label, boolean initialValue, Consumer<Boolean> onChange) {
        this(x, y, width, label, null, initialValue, onChange);
    }

    public ToggleSwitch(int x, int y, int width, String label, String description, boolean initialValue,
            Consumer<Boolean> onChange) {
        super(x, y, width, SWITCH_HEIGHT);
        this.label = label;
        this.description = description;
        this.value = initialValue;
        this.onChange = onChange;

        this.toggleAnimation = new Animation(initialValue ? 1 : 0, initialValue ? 1 : 0, Theme.ANIM_NORMAL,
                Easing::easeOut);
        this.hoverAnimation = new Animation(0, 0, Theme.ANIM_HOVER, Easing::easeOut);
        this.expandAnimation = new Animation(0, 0, Theme.ANIM_NORMAL, Easing::easeOut);

        this.labelWidth = Minecraft.getInstance().font.width(label);

        updateHeight();
    }

    private void updateHeight() {
        int baseHeight = SWITCH_HEIGHT;
        if (description != null && !description.isEmpty() && (expanded || expandAnimation.getValue() > 0)) {
            int lines = 1 + (int) Math.ceil((double) description.length() / 40);
            descriptionHeight = lines * 10;
            this.height = baseHeight + (int) (descriptionHeight * expandAnimation.getValue()) + 10;
        } else {
            this.height = baseHeight;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible)
            return;

        int textY = y + (SWITCH_HEIGHT - 8) / 2;
        graphics.text(Minecraft.getInstance().font, label, x, textY, labelColor);

        if (description != null && !description.isEmpty()) {
            int expandX = x + labelWidth + 6;
            int expandY = y + (SWITCH_HEIGHT - EXPAND_ICON_SIZE) / 2;
            int expandColor = Theme.withAlpha(Theme.TEXT_SECONDARY, 0.6f);

            graphics.text(Minecraft.getInstance().font, expandAnimation.getValue() > 0.5f ? "▼" : "▶", expandX, expandY, expandColor);
        }

        renderSwitch(graphics, mouseX, mouseY);
    }

    @Override
    public void extractRenderOverlay(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int rawMouseX, int rawMouseY,
            float partialTick) {
        if (!visible)
            return;
        renderDescription(graphics);
    }

    private void renderDescription(GuiGraphicsExtractor graphics) {
        float expandProgress = expandAnimation.getValue();
        if (description == null || description.isEmpty() || expandProgress <= 0) {
            return;
        }

        int descY = y + SWITCH_HEIGHT + 5;
        int visibleDescHeight = Math.max(1, (int) (descriptionHeight * expandProgress));
        int descColor = Theme.withAlpha(Theme.TEXT_SECONDARY, expandProgress);

        graphics.enableScissor(x, descY, x + width, descY + visibleDescHeight);
        String[] words = description.split(" ");
        StringBuilder line = new StringBuilder();

        for (String word : words) {
            if (Minecraft.getInstance().font.width(line + word) > width - 20 && !line.isEmpty()) {
                graphics.text(Minecraft.getInstance().font, line.toString().trim(), x + 10, descY, descColor);
                descY += 10;
                line = new StringBuilder();
            }
            line.append(word).append(" ");
        }
        if (!line.isEmpty()) {
            graphics.text(Minecraft.getInstance().font, line.toString().trim(), x + 10, descY, descColor);
        }
        graphics.disableScissor();
    }

    private void renderSwitch(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        float toggleProgress = toggleAnimation.getValue();
        float hoverProgress = hoverAnimation.getValue();

        int switchX = x + width - SWITCH_WIDTH;
        int switchY = y;
        int bgColor = value ? Theme.withAlpha(Theme.ACCENT, 0.3f + hoverProgress * 0.2f)
                : Theme.withAlpha(Theme.SURFACE_LIGHT, 1.0f);

        RenderHelper.renderRoundedRect(graphics, switchX, switchY, SWITCH_WIDTH, SWITCH_HEIGHT,
                SWITCH_HEIGHT / 2, bgColor);

        int thumbX = switchX + 2 + (int) (toggleProgress * (SWITCH_WIDTH - THUMB_SIZE - 4));
        int thumbY = switchY + (SWITCH_HEIGHT - THUMB_SIZE) / 2;
        int thumbColor = value
                ? Theme.withAlpha(Theme.ACCENT, 1.0f)
                : Theme.TEXT_SECONDARY;

        RenderHelper.renderRoundedRect(graphics, thumbX, thumbY, THUMB_SIZE, THUMB_SIZE, THUMB_SIZE / 2, thumbColor);
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

        updateHeight();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, MouseButtonEvent event) {
        if (!enabled || !visible)
            return false;

        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }

        if (description != null && !description.isEmpty()) {
            int expandX = x + labelWidth + 6;
            int expandY = y;
            int expandWidth = 12;
            if (mouseX >= expandX && mouseX <= expandX + expandWidth &&
                    mouseY >= expandY && mouseY <= expandY + SWITCH_HEIGHT) {
                toggleExpand();
                return true;
            }
        }

        int switchX = x + width - SWITCH_WIDTH;
        if (mouseX >= switchX && mouseX <= switchX + SWITCH_WIDTH &&
                mouseY >= y && mouseY <= y + SWITCH_HEIGHT) {
            toggle();
            return true;
        }

        return true;
    }

    private void toggle() {
        this.value = !this.value;
        toggleAnimation = new Animation(toggleAnimation.getValue(), value ? 1 : 0, Theme.ANIM_NORMAL, Easing::easeOut);
        toggleAnimation.start();

        if (onChange != null) {
            onChange.accept(value);
        }
    }

    private void toggleExpand() {
        this.expanded = !this.expanded;
        expandAnimation = new Animation(expandAnimation.getValue(), expanded ? 1 : 0, Theme.ANIM_NORMAL,
                Easing::easeOut);
        expandAnimation.start();
        if (onExpandChange != null) {
            onExpandChange.run();
        }
    }

    public boolean getValue() {
        return value;
    }

    public void setValue(boolean value) {
        if (this.value != value) {
            this.value = value;
            toggleAnimation = new Animation(toggleAnimation.getValue(), value ? 1 : 0, Theme.ANIM_NORMAL,
                    Easing::easeOut);
            toggleAnimation.start();
        }
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
        this.labelWidth = Minecraft.getInstance().font.width(label);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        if (this.expanded != expanded) {
            toggleExpand();
        }
    }

    public void setOnExpandChange(Runnable onExpandChange) {
        this.onExpandChange = onExpandChange;
    }

    public void setLabelColor(int color) {
        this.labelColor = color;
    }
}
