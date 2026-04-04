package org.blackum.blackaddons.gui.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
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

    private boolean isItemWithinViewport(Widget item) {
        return item.isVisible() && item.getY() + item.getHeight() > y && item.getY() < y + height;
    }

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

                if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height
                        && currentY + item.getHeight() > y && currentY < y + height) {
                    item.updateHoverState(mouseX, mouseY);
                } else {
                    item.updateHoverState(-1, -1);
                }

                currentY += item.getHeight() + itemSpacing;
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible)
            return;

        graphics.enableScissor(x, y, x + width, y + height);
        int currentY = y - scrollOffset;
        for (Widget item : items) {
            if (item.isVisible()) {
                item.setX(x);
                item.setY(currentY);
                item.setWidth(width - scrollbarWidth - 12);

                item.extractRenderState(graphics, mouseX, mouseY, partialTick);

                currentY += item.getHeight() + itemSpacing;
            }
        }
        graphics.disableScissor();

        renderScrollbar(graphics, mouseX, mouseY);
    }

    @Override
    public void extractRenderOverlay(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int rawMouseX, int rawMouseY,
            float partialTick) {
        if (!visible || !(super.isMouseOver(mouseX, mouseY) || hasActiveOverlay()))
            return;

        int currentY = y - scrollOffset;
        for (Widget item : items) {
            if (item.isVisible() && currentY + item.getHeight() > y && currentY < y + height) {
                item.setX(x);
                item.setY(currentY);
                item.setWidth(width - scrollbarWidth - 12);

                item.extractRenderOverlay(graphics, mouseX, mouseY, rawMouseX, rawMouseY, partialTick);
            }
            currentY += item.getHeight() + itemSpacing;
        }
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (!super.isMouseOver(mouseX, mouseY)) {
            return false;
        }
        for (Widget item : items) {
            if (isItemWithinViewport(item) && item.isMouseOver(mouseX, mouseY)) {
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean hasActiveOverlay() {
        for (Widget item : items) {
            if (item.isVisible() && item.hasActiveOverlay()) {
                return true;
            }
        }
        return false;
    }

    private void renderScrollbar(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
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
    public boolean mouseClicked(double mouseX, double mouseY, MouseButtonEvent event) {
        if (!enabled || !visible || !isMouseOver(mouseX, mouseY))
            return false;

        if (isMouseOverScrollbar(mouseX, mouseY)) {
            draggingScrollbar = true;
            updateScrollFromMouse(mouseY);
            return true;
        }

        for (Widget item : items) {
            if (isItemWithinViewport(item)) {
                if (item.mouseClicked(mouseX, mouseY, event)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, MouseButtonEvent event) {
        draggingScrollbar = false;

        for (Widget item : items) {
            if (isItemWithinViewport(item) && item.mouseReleased(mouseX, mouseY, event)) {
                return true;
            }
        }

        return super.mouseReleased(mouseX, mouseY, event);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, MouseButtonEvent event, double dragX, double dragY) {
        if (draggingScrollbar) {
            updateScrollFromMouse(mouseY);
            return true;
        }

        for (Widget item : items) {
            if (isItemWithinViewport(item) && item.mouseDragged(mouseX, mouseY, event, dragX, dragY)) {
                return true;
            }
        }

        return super.mouseDragged(mouseX, mouseY, event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, MouseButtonEvent event, double scrollX, double scrollY) {
        if (!visible)
            return false;

        double verticalAmount = scrollY;
        for (Widget item : items) {
            if (isItemWithinViewport(item) && item.mouseScrolled(mouseX, mouseY, event, scrollX, scrollY)) {
                return true;
            }
        }

        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            scroll((int) (-verticalAmount * 20));
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, KeyEvent event) {
        for (Widget item : items) {
            if (isItemWithinViewport(item) && item.keyPressed(keyCode, scanCode, event)) {
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, event);
    }

    @Override
    public boolean charTyped(char chr, CharacterEvent event) {
        for (Widget item : items) {
            if (isItemWithinViewport(item) && item.charTyped(chr, event)) {
                return true;
            }
        }
        return super.charTyped(chr, event);
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
        notifyItemsScrolled();
    }

    public void scrollTo(int offset) {
        this.scrollOffset = Math.max(0, Math.min(getMaxScroll(), offset));
        notifyItemsScrolled();
    }

    private void scroll(int delta) {
        scrollOffset += delta;
        updateMaxScroll();
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset));
        notifyItemsScrolled();
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
            totalHeight += 5;
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
        notifyItemsScrolled();
    }

    private void notifyItemsScrolled() {
        for (Widget item : items) {
            if (item.isVisible()) {
                item.onScrolled();
            }
        }
    }

    public int getScrollOffset() {
        return scrollOffset;
    }

    public boolean isAtBottom() {
        return scrollOffset >= getMaxScroll();
    }
}
