package org.blackum.blackaddons.gui.widget;

import net.minecraft.client.gui.GuiGraphics;
import org.blackum.blackaddons.gui.render.Theme;

import java.util.ArrayList;
import java.util.List;

public class ListView extends Widget {

    private List<Widget> items = new ArrayList<>();
    private int scrollOffset = 0;
    private int itemSpacing = 8;
    private int maxScroll = 0;
    private boolean draggingScrollbar = false;
    private int scrollbarWidth = 4;

    public ListView(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    @Override
    public void updateHoverState(int mouseX, int mouseY) {
        super.updateHoverState(mouseX, mouseY);

        int currentY = y - scrollOffset;
        for (Widget item : items) {
            if (item.isVisible()) {
                item.setX(x);
                item.setY(currentY);
                item.setWidth(width - scrollbarWidth - 12);

                if (isMouseOver(mouseX, mouseY) && currentY + item.getHeight() >= y && currentY <= y + height) {
                    item.updateHoverState(mouseX, mouseY);
                } else {
                    item.updateHoverState(-1, -1);
                }

                currentY += item.getHeight() + itemSpacing;
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible)
            return;

        graphics.enableScissor(x, y, x + width, y + height);
        int currentY = y - scrollOffset;
        for (Widget item : items) {
            if (item.isVisible()) {
                item.setX(x);
                item.setY(currentY);
                item.setWidth(width - scrollbarWidth - 12);

                item.render(graphics, mouseX, mouseY, partialTick);

                currentY += item.getHeight() + itemSpacing;
            }
        }
        graphics.disableScissor();

        renderScrollbar(graphics, mouseX, mouseY);
    }

    @Override
    public void renderOverlay(GuiGraphics graphics, int mouseX, int mouseY, int rawMouseX, int rawMouseY,
            float partialTick) {
        if (!visible)
            return;

        int currentY = y - scrollOffset;
        for (Widget item : items) {
            if (item.isVisible()) {
                item.setX(x);
                item.setY(currentY);
                item.setWidth(width - scrollbarWidth - 12);

                item.renderOverlay(graphics, mouseX, mouseY, rawMouseX, rawMouseY, partialTick);

                currentY += item.getHeight() + itemSpacing;
            }
        }
    }

    private void renderScrollbar(GuiGraphics graphics, int mouseX, int mouseY) {
        updateMaxScroll();

        if (maxScroll <= 0)
            return;

        int scrollbarX = x + width - scrollbarWidth;
        int scrollbarHeight = height;

        int thumbHeight = Math.max(20, (int) ((float) height / (height + maxScroll) * scrollbarHeight));
        int thumbY = y + (int) ((float) scrollOffset / maxScroll * (scrollbarHeight - thumbHeight));

        int thumbColor = Theme.withAlpha(Theme.TEXT_SECONDARY,
                (draggingScrollbar || isMouseOverScrollbar(mouseX, mouseY)) ? 0.8f : 0.4f);
        graphics.fill(scrollbarX, thumbY, scrollbarX + scrollbarWidth, thumbY + thumbHeight, thumbColor);
    }

    private boolean isMouseOverScrollbar(double mouseX, double mouseY) {
        int scrollbarX = x + width - scrollbarWidth;
        return mouseX >= scrollbarX && mouseX <= scrollbarX + scrollbarWidth &&
                mouseY >= y && mouseY <= y + height;
    }

    @Override
    public void tick() {
        for (Widget item : items) {
            item.tick();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!enabled || !visible || !isMouseOver(mouseX, mouseY))
            return false;

        if (isMouseOverScrollbar(mouseX, mouseY)) {
            draggingScrollbar = true;
            updateScrollFromMouse(mouseY);
            return true;
        }

        for (Widget item : items) {
            if (item.isVisible() && item.getY() + item.getHeight() > y && item.getY() < y + height) {
                if (item.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingScrollbar = false;

        for (Widget item : items) {
            if (item.isVisible() && item.mouseReleased(mouseX, mouseY, button)) {
                return true;
            }
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScrollbar) {
            updateScrollFromMouse(mouseY);
            return true;
        }

        for (Widget item : items) {
            if (item.isVisible() && item.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
                return true;
            }
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!visible)
            return false;

        for (Widget item : items) {
            if (item.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
                return true;
            }
        }

        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            scroll((int) (-scrollY * 20));
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (Widget item : items) {
            if (item.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        for (Widget item : items) {
            if (item.charTyped(character, modifiers)) {
                return true;
            }
        }
        return super.charTyped(character, modifiers);
    }

    private void updateScrollFromMouse(double mouseY) {
        updateMaxScroll();
        if (maxScroll <= 0)
            return;

        int scrollbarHeight = height;
        int thumbHeight = Math.max(20, (int) ((float) height / (height + maxScroll) * scrollbarHeight));
        int scrollableHeight = scrollbarHeight - thumbHeight;

        float mouseProgress = (float) (mouseY - y) / scrollableHeight;
        scrollOffset = (int) (mouseProgress * maxScroll);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset));
    }

    public void scrollTo(int offset) {
        this.scrollOffset = Math.max(0, Math.min(getMaxScroll(), offset));
    }

    private void scroll(int delta) {
        scrollOffset += delta;
        updateMaxScroll();
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset));
    }

    private void updateMaxScroll() {
        int totalHeight = 0;
        for (Widget item : items) {
            if (item.isVisible()) {
                totalHeight += item.getHeight() + itemSpacing;
            }
        }
        if (totalHeight > 0) {
            totalHeight -= itemSpacing;
        }
        maxScroll = Math.max(0, totalHeight - height);
    }

    public void addItem(Widget widget) {
        items.add(widget);
    }

    public void removeItem(Widget widget) {
        items.remove(widget);
    }

    public void clearItems() {
        items.clear();
    }

    public List<Widget> getItems() {
        return items;
    }

    public int getItemSpacing() {
        return itemSpacing;
    }

    public void setItemSpacing(int itemSpacing) {
        this.itemSpacing = itemSpacing;
    }

    public int getMaxScroll() {
        updateMaxScroll();
        return maxScroll;
    }

    public void setScrollOffset(int scrollOffset) {
        this.scrollOffset = scrollOffset;
    }

    public int getScrollOffset() {
        return scrollOffset;
    }

    public boolean isAtBottom() {
        return scrollOffset >= getMaxScroll();
    }
}
