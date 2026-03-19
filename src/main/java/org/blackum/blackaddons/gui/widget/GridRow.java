package org.blackum.blackaddons.gui.widget;

import net.minecraft.client.gui.GuiGraphics;
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
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible)
            return;
        for (Map.Entry<Widget, Integer> entry : children) {
            entry.getKey().render(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public void renderOverlay(GuiGraphics graphics, int mouseX, int mouseY, int rawMouseX, int rawMouseY,
            float partialTick) {
        if (!visible)
            return;
        for (Map.Entry<Widget, Integer> entry : children) {
            Widget widget = entry.getKey();
            if (widget.isVisible()) {
                widget.renderOverlay(graphics, mouseX, mouseY, rawMouseX, rawMouseY, partialTick);
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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!enabled || !visible)
            return false;
        for (Map.Entry<Widget, Integer> entry : children) {
            if (entry.getKey().mouseClicked(mouseX, mouseY, button))
                return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!enabled || !visible)
            return false;
        for (Map.Entry<Widget, Integer> entry : children) {
            if (entry.getKey().mouseReleased(mouseX, mouseY, button))
                return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!enabled || !visible)
            return false;
        for (Map.Entry<Widget, Integer> entry : children) {
            if (entry.getKey().mouseDragged(mouseX, mouseY, button, dragX, dragY))
                return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!visible)
            return false;
        for (Map.Entry<Widget, Integer> entry : children) {
            if (entry.getKey().mouseScrolled(mouseX, mouseY, scrollX, scrollY))
                return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!enabled || !visible)
            return false;
        for (Map.Entry<Widget, Integer> entry : children) {
            if (entry.getKey().keyPressed(keyCode, scanCode, modifiers))
                return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        if (!enabled || !visible)
            return false;
        for (Map.Entry<Widget, Integer> entry : children) {
            if (entry.getKey().charTyped(character, modifiers))
                return true;
        }
        return super.charTyped(character, modifiers);
    }
}
