package org.blackum.blackaddons.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.blackum.blackaddons.gui.animation.Animation;
import org.blackum.blackaddons.gui.animation.Easing;
import org.blackum.blackaddons.gui.render.Theme;

public class SettingWrapper extends Widget {

    private static final int EXPAND_ICON_SIZE = 8;
    private static final int HEADER_HEIGHT = 20;

    private String label;
    private String description;
    private Widget control;
    private boolean expanded = false;
    private Animation expandAnimation;

    private String rightLabel;
    private int labelWidth;
    private int descriptionHeight = 0;

    public void setRightLabel(String rightLabel) {
        this.rightLabel = rightLabel;
    }

    public SettingWrapper(int x, int y, int width, String label, String description, Widget control) {
        super(x, y, width, HEADER_HEIGHT);
        this.label = label;
        this.description = description;
        this.control = control;

        this.expandAnimation = new Animation(0, 0, Theme.ANIM_NORMAL, Easing::easeOut);
        this.labelWidth = Minecraft.getInstance().font.width(label);

        updateLayout();
    }

    private void updateLayout() {
        int currentDescHeight = 0;
        if (description != null && !description.isEmpty()) {
            if (expanded) {
                int lines = 1 + (int) Math.ceil((double) description.length() / 40);
                descriptionHeight = lines * 10;
            }
            currentDescHeight = (int) (descriptionHeight * expandAnimation.getValue());
        }

        if (control != null) {
            control.setX(x);
            control.setY(y + HEADER_HEIGHT + 2 + currentDescHeight);
            this.height = HEADER_HEIGHT + 2 + currentDescHeight + control.getHeight() + 4;
        } else {
            this.height = HEADER_HEIGHT + currentDescHeight;
        }
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        updateLayout();
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        updateLayout();
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        if (control != null) {
            control.setWidth(width);
        }
        updateLayout();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible)
            return;

        int textY = y + (HEADER_HEIGHT - 8) / 2;
        graphics.drawString(Minecraft.getInstance().font, label, x, textY, Theme.TEXT_PRIMARY);

        if (rightLabel != null && !rightLabel.isEmpty()) {
            int rw = Minecraft.getInstance().font.width(rightLabel);
            graphics.drawString(Minecraft.getInstance().font, rightLabel, x + width - rw - 10, textY,
                    Theme.TEXT_SECONDARY);
        }

        if (description != null && !description.isEmpty()) {
            int expandX = x + labelWidth + 6;
            int expandY = y + (HEADER_HEIGHT - EXPAND_ICON_SIZE) / 2;
            int expandColor = Theme.withAlpha(Theme.TEXT_SECONDARY, 0.6f);
            graphics.drawString(Minecraft.getInstance().font, expanded ? "▼" : "▶", expandX, expandY, expandColor);
        }

        if (description != null && !description.isEmpty() && expandAnimation.getValue() > 0) {
            int descY = y + HEADER_HEIGHT + 2;
            float alpha = expandAnimation.getValue();
            int descColor = Theme.withAlpha(Theme.TEXT_SECONDARY, alpha);

            String[] words = description.split(" ");
            StringBuilder line = new StringBuilder();

            for (String word : words) {
                if (Minecraft.getInstance().font.width(line + word) > width - 20 && !line.isEmpty()) {
                    graphics.drawString(Minecraft.getInstance().font, line.toString().trim(), x + 10, descY, descColor);
                    descY += 10;
                    line = new StringBuilder();
                }
                line.append(word).append(" ");
            }
            if (!line.isEmpty()) {
                graphics.drawString(Minecraft.getInstance().font, line.toString().trim(), x + 10, descY, descColor);
            }
        }

        if (control != null && control.isVisible()) {
            control.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public void renderOverlay(GuiGraphics graphics, int mouseX, int mouseY, int rawMouseX, int rawMouseY,
            float partialTick) {
        if (!visible)
            return;
        if (control != null && control.isVisible()) {
            control.renderOverlay(graphics, mouseX, mouseY, rawMouseX, rawMouseY, partialTick);
        }
    }

    @Override
    public void tick() {
        if (control != null)
            control.tick();
        updateLayout();
    }

    @Override
    public void updateHoverState(int mouseX, int mouseY) {
        super.updateHoverState(mouseX, mouseY);
        if (control != null) {
            control.updateHoverState(mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!enabled || !visible)
            return false;

        if (control != null && control.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        if (description != null && !description.isEmpty()) {
            int expandX = x + labelWidth + 6;
            int expandWidth = 12;
            if (mouseX >= expandX && mouseX <= expandX + expandWidth && mouseY >= y && mouseY <= y + 16) {
                toggleExpand();
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!enabled || !visible)
            return false;
        if (control != null)
            return control.mouseReleased(mouseX, mouseY, button);
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!enabled || !visible)
            return false;
        if (control != null)
            return control.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!visible)
            return false;
        if (control != null)
            return control.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!enabled || !visible)
            return false;
        if (control != null)
            return control.keyPressed(keyCode, scanCode, modifiers);
        return false;
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        if (!enabled || !visible)
            return false;
        if (control != null)
            return control.charTyped(character, modifiers);
        return false;
    }

    private void toggleExpand() {
        this.expanded = !this.expanded;
        expandAnimation = new Animation(expandAnimation.getValue(), expanded ? 1 : 0, Theme.ANIM_NORMAL,
                Easing::easeOut);
        expandAnimation.start();
    }
}
