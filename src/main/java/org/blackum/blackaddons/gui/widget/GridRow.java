package org.blackum.blackaddons.gui.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GridRow extends Widget {
    private final List<Map.Entry<Widget, Integer>> children = new ArrayList<>();

    public GridRow(int w, int h) {
        super(0, 0, w, h);
    }

    public void addChild(Widget w, int xOffset) {
        children.add(new AbstractMap.SimpleEntry<>(w, xOffset));
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        for (Map.Entry<Widget, Integer> entry : children) {
            entry.getKey().setX(x + entry.getValue());
        }
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        for (Map.Entry<Widget, Integer> entry : children) {
            entry.getKey().setY(y);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible)
            return;
        for (Map.Entry<Widget, Integer> entry : children) {
            entry.getKey().extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public void extractRenderOverlay(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int rawMouseX, int rawMouseY,
            float partialTick) {
        if (!visible)
            return;
        for (Map.Entry<Widget, Integer> entry : children) {
            Widget widget = entry.getKey();
            if (widget.isVisible()) {
                widget.extractRenderOverlay(graphics, mouseX, mouseY, rawMouseX, rawMouseY, partialTick);
            }
        }
    }

    @Override
    public void updateHoverState(int mouseX, int mouseY) {
        super.updateHoverState(mouseX, mouseY);
        for (Map.Entry<Widget, Integer> entry : children) {
            entry.getKey().updateHoverState(mouseX, mouseY);
        }
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (super.isMouseOver(mouseX, mouseY)) {
            return true;
        }
        for (Map.Entry<Widget, Integer> entry : children) {
            Widget child = entry.getKey();
            if (child.isVisible() && child.isMouseOver(mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasActiveOverlay() {
        for (Map.Entry<Widget, Integer> entry : children) {
            Widget child = entry.getKey();
            if (child.isVisible() && child.hasActiveOverlay()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void tick() {
        for (Map.Entry<Widget, Integer> entry : children) {
            entry.getKey().tick();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, MouseButtonEvent event) {
        if (!enabled || !visible)
            return false;
        for (Map.Entry<Widget, Integer> entry : children) {
            Widget widget = entry.getKey();
            if (widget.isVisible() && widget.mouseClicked(mouseX, mouseY, event)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, MouseButtonEvent event) {
        if (!enabled || !visible)
            return false;
        for (Map.Entry<Widget, Integer> entry : children) {
            if (entry.getKey().mouseReleased(mouseX, mouseY, event))
                return true;
        }
        return super.mouseReleased(mouseX, mouseY, event);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, MouseButtonEvent event, double dragX, double dragY) {
        if (!enabled || !visible)
            return false;
        for (Map.Entry<Widget, Integer> entry : children) {
            if (entry.getKey().mouseDragged(mouseX, mouseY, event, dragX, dragY))
                return true;
        }
        return super.mouseDragged(mouseX, mouseY, event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, MouseButtonEvent event, double scrollX, double scrollY) {
        if (!visible)
            return false;
        for (Map.Entry<Widget, Integer> entry : children) {
            if (entry.getKey().mouseScrolled(mouseX, mouseY, event, scrollX, scrollY))
                return true;
        }
        return super.mouseScrolled(mouseX, mouseY, event, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, KeyEvent event) {
        if (!enabled || !visible)
            return false;
        for (Map.Entry<Widget, Integer> entry : children) {
            if (entry.getKey().keyPressed(keyCode, scanCode, event))
                return true;
        }
        return super.keyPressed(keyCode, scanCode, event);
    }

    @Override
    public boolean charTyped(char chr, CharacterEvent event) {
        if (!enabled || !visible)
            return false;
        for (Map.Entry<Widget, Integer> entry : children) {
            if (entry.getKey().charTyped(chr, event)) {
                return true;
            }
        }
        return super.charTyped(chr, event);
    }
}
