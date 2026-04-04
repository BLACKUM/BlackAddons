package org.blackum.blackaddons.gui.widget;

import net.minecraft.client.input.KeyEvent;

import net.minecraft.client.input.MouseButtonEvent;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.render.RenderHelper;

import java.util.ArrayList;
import java.util.List;

public class Card extends Widget {

    private String title;
    private List<Widget> children = new ArrayList<>();
    private int padding = Theme.PADDING;

    public Card(int x, int y, int width, int height) {
        this(x, y, width, height, null);
    }

    public Card(int x, int y, int width, int height, String title) {
        super(x, y, width, height);
        this.title = title;
    }

    @Override
    public void updateHoverState(int mouseX, int mouseY) {
        super.updateHoverState(mouseX, mouseY);
        for (Widget child : children) {
            if (child.isVisible()) {
                child.updateHoverState(mouseX, mouseY);
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible)
            return;

        RenderHelper.renderSurface(graphics, x, y, width, height, Theme.BORDER_RADIUS, false);

        int contentY = y + padding;

        if (title != null && !title.isEmpty()) {
            graphics.text(Minecraft.getInstance().font,
                    title, x + padding, contentY, Theme.TEXT_PRIMARY);
            contentY += 12;
        }

        for (Widget child : children) {
            if (child.isVisible()) {
                child.extractRenderState(graphics, mouseX, mouseY, partialTick);
            }
        }
    }

    @Override
    public void extractRenderOverlay(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int rawMouseX, int rawMouseY,
            float partialTick) {
        if (!visible)
            return;
        for (Widget child : children) {
            if (child.isVisible()) {
                child.extractRenderOverlay(graphics, mouseX, mouseY, rawMouseX, rawMouseY, partialTick);
            }
        }
    }

    @Override
    public void tick() {
        for (Widget child : children) {
            child.tick();
        }
    }

    @Override
    public void onScrolled() {
        for (Widget child : children) {
            child.onScrolled();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, MouseButtonEvent event) {
        if (!enabled || !visible)
            return false;

        for (int i = children.size() - 1; i >= 0; i--) {
            Widget child = children.get(i);
            if (child.mouseClicked(mouseX, mouseY, event)) {
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, event) || isMouseOver(mouseX, mouseY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, MouseButtonEvent event) {
        for (Widget child : children) {
            if (child.mouseReleased(mouseX, mouseY, event)) {
                return true;
            }
        }
        return super.mouseReleased(mouseX, mouseY, event);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, MouseButtonEvent event, double dragX, double dragY) {
        for (Widget child : children) {
            if (child.mouseDragged(mouseX, mouseY, event, dragX, dragY)) {
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, event, dragX, dragY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, KeyEvent event) {
        for (Widget child : children) {
            if (child.keyPressed(keyCode, scanCode, event)) {
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, event);
    }

    @Override
    public boolean charTyped(char chr, CharacterEvent event) {
        for (Widget child : children) {
            if (child.charTyped(chr, event)) {
                return true;
            }
        }
        return super.charTyped(chr, event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, MouseButtonEvent event, double scrollX, double scrollY) {
        for (Widget child : children) {
            if (child.mouseScrolled(mouseX, mouseY, event, scrollX, scrollY)) {
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, event, scrollX, scrollY);
    }

    public void addChild(Widget widget) {
        children.add(widget);
    }

    public void removeChild(Widget widget) {
        children.remove(widget);
    }

    public void clearChildren() {
        children.clear();
    }

    public List<Widget> getChildren() {
        return children;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getPadding() {
        return padding;
    }

    public void setPadding(int padding) {
        this.padding = padding;
    }

    public int getContentX() {
        return x + padding;
    }

    public int getContentY() {
        int contentY = y + padding;
        if (title != null && !title.isEmpty()) {
            contentY += 12;
        }
        return contentY;
    }

    public int getContentWidth() {
        return width - padding * 2;
    }

    public int getContentHeight() {
        int usedHeight = padding;
        if (title != null && !title.isEmpty()) {
            usedHeight += 12;
        }
        return height - usedHeight - padding;
    }
}
