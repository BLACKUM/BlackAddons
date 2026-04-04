package org.blackum.blackaddons.gui.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.blackum.blackaddons.gui.animation.Animation;
import org.blackum.blackaddons.gui.animation.Easing;
import org.blackum.blackaddons.gui.render.Theme;

import java.util.ArrayList;
import java.util.List;

public class ExpandableGroup extends Widget {
    private final SectionHeader header;
    private final List<Widget> children = new ArrayList<>();
    private Animation expandAnimation;
    private boolean expanded;

    public ExpandableGroup(int x, int y, int width, SectionHeader header, boolean expanded) {
        super(x, y, width, header.getHeight());
        this.header = header;
        this.expanded = expanded;
        this.expandAnimation = new Animation(expanded ? 1.0f : 0.0f, expanded ? 1.0f : 0.0f, Theme.ANIM_NORMAL,
                Easing::easeOut);

        header.setX(x);
        header.setY(y);
    }

    public void addChild(Widget widget) {
        children.add(widget);
        updateLayout();
    }

    private void updateLayout() {
        if (!expanded && expandAnimation.getValue() == 0) {
            header.setX(x);
            header.setY(y);
            this.height = header.getHeight();
            return;
        }

        int currentY = y + header.getHeight() + 4;
        for (Widget child : children) {
            child.setX(x);
            child.setY(currentY);
            currentY += child.getHeight() + 4;
        }

        int childrenHeight = currentY - (y + header.getHeight() + 4);
        int animatedChildrenHeight = (int) (childrenHeight * expandAnimation.getValue());
        this.height = header.getHeight() + 4 + animatedChildrenHeight;
    }

    public void setExpanded(boolean expanded) {
        if (this.expanded == expanded)
            return;
        this.expanded = expanded;
        this.expandAnimation = new Animation(expanded ? 0.0f : 1.0f, expanded ? 1.0f : 0.0f, Theme.ANIM_NORMAL,
                Easing::easeOut);
        this.expandAnimation.start();
    }

    public boolean isExpanded() {
        return expanded;
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        header.setX(x);
        updateLayout();
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        header.setY(y);
        updateLayout();
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        header.setWidth(width);
        for (Widget child : children) {
            child.setWidth(width);
        }
        updateLayout();
    }

    @Override
    public void tick() {
        header.tick();
        if (expanded || expandAnimation.getValue() > 0) {
            for (Widget child : children) {
                child.tick();
            }
        }
        updateLayout();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible)
            return;

        header.extractRenderState(graphics, mouseX, mouseY, partialTick);

        if (expandAnimation.getValue() > 0) {
            boolean scissored = false;
            if (expandAnimation.getValue() < 1.0f) {
                graphics.enableScissor(x, y + header.getHeight(), x + width, y + height);
                scissored = true;
            }

            for (Widget child : children) {
                child.extractRenderState(graphics, mouseX, mouseY, partialTick);
            }

            if (scissored) {
                graphics.disableScissor();
            }
        }
    }

    @Override
    public void extractRenderOverlay(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int rawMouseX, int rawMouseY,
            float partialTick) {
        if (!visible || expandAnimation.getValue() == 0)
            return;

        for (Widget child : children) {
            child.extractRenderOverlay(graphics, mouseX, mouseY, rawMouseX, rawMouseY, partialTick);
        }
    }

    @Override
    public void updateHoverState(int mouseX, int mouseY) {
        super.updateHoverState(mouseX, mouseY);
        header.updateHoverState(mouseX, mouseY);

        if (expanded || expandAnimation.getValue() > 0) {
            for (Widget child : children) {
                child.updateHoverState(mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (super.isMouseOver(mouseX, mouseY) || header.isMouseOver(mouseX, mouseY)) {
            return true;
        }
        if (expanded || expandAnimation.getValue() > 0) {
            for (Widget child : children) {
                if (child.isVisible() && child.isMouseOver(mouseX, mouseY)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean hasActiveOverlay() {
        if (!(expanded || expandAnimation.getValue() > 0)) {
            return false;
        }
        for (Widget child : children) {
            if (child.isVisible() && child.hasActiveOverlay()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, MouseButtonEvent event) {
        if (!enabled || !visible)
            return false;

        if (header.mouseClicked(mouseX, mouseY, event)) {
            return true;
        }

        if (expanded && expandAnimation.getValue() == 1.0f) {
            for (Widget child : children) {
                if (child.mouseClicked(mouseX, mouseY, event)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, MouseButtonEvent event) {
        if (!enabled || !visible)
            return false;

        boolean handled = header.mouseReleased(mouseX, mouseY, event);
        if (expanded && expandAnimation.getValue() == 1.0f) {
            for (Widget child : children) {
                if (child.mouseReleased(mouseX, mouseY, event)) {
                    handled = true;
                }
            }
        }
        return handled;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, MouseButtonEvent event, double dragX, double dragY) {
        if (!enabled || !visible)
            return false;

        boolean handled = header.mouseDragged(mouseX, mouseY, event, dragX, dragY);
        if (expanded && expandAnimation.getValue() == 1.0f) {
            for (Widget child : children) {
                if (child.mouseDragged(mouseX, mouseY, event, dragX, dragY)) {
                    handled = true;
                }
            }
        }
        return handled;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, MouseButtonEvent event, double scrollX, double scrollY) {
        if (!visible)
            return false;

        boolean handled = header.mouseScrolled(mouseX, mouseY, event, scrollX, scrollY);
        if (expanded && expandAnimation.getValue() == 1.0f) {
            for (Widget child : children) {
                if (child.mouseScrolled(mouseX, mouseY, event, scrollX, scrollY)) {
                    handled = true;
                }
            }
        }
        return handled;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, KeyEvent event) {
        if (!enabled || !visible)
            return false;

        boolean handled = header.keyPressed(keyCode, scanCode, event);
        if (expanded && expandAnimation.getValue() == 1.0f) {
            for (Widget child : children) {
                if (child.keyPressed(keyCode, scanCode, event)) {
                    handled = true;
                }
            }
        }
        return handled;
    }

    @Override
    public boolean charTyped(char chr, CharacterEvent event) {
        if (!enabled || !visible)
            return false;

        boolean handled = header.charTyped(chr, event);
        if (expanded && expandAnimation.getValue() == 1.0f) {
            for (Widget child : children) {
                if (child.charTyped(chr, event)) {
                    handled = true;
                }
            }
        }
        return handled;
    }
}
