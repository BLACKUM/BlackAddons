package org.blackum.blackaddons.gui.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.render.RenderHelper;

public class ResizableCard extends Card {

    private static final int RESIZE_HANDLE_SIZE = 12;
    private static final int TITLE_BAR_HEIGHT = 24;

    private boolean dragging = false;
    private boolean resizing = false;
    private int dragStartX = 0;
    private int dragStartY = 0;
    private int dragStartCardX = 0;
    private int dragStartCardY = 0;
    private int dragStartWidth = 0;
    private ResizeHandle activeHandle = ResizeHandle.NONE;

    private int initialWidth;

    private boolean collapsed = true;
    private int expandedHeight;

    private int minX = Integer.MIN_VALUE;
    private int minY = Integer.MIN_VALUE;
    private int maxX = Integer.MAX_VALUE;
    private int maxY = Integer.MAX_VALUE;

    private enum ResizeHandle {
        NONE, BOTTOM_RIGHT, BOTTOM, RIGHT
    }

    private Runnable onLayoutChange;

    public void setOnLayoutChange(Runnable onLayoutChange) {
        this.onLayoutChange = onLayoutChange;
    }

    public ResizableCard(int x, int y, int width, int height, String title) {
        super(x, y, width, height, title);
        this.initialWidth = width;
        this.expandedHeight = height;

        if (collapsed) {
            this.height = TITLE_BAR_HEIGHT;
        }
    }

    public boolean isCollapsed() {
        return collapsed;
    }

    public void setCollapsed(boolean collapsed) {
        if (this.collapsed == collapsed)
            return;
        this.collapsed = collapsed;
        if (collapsed) {
            this.expandedHeight = this.height;
            this.height = TITLE_BAR_HEIGHT;
        } else {
            updateLayout();
        }
    }

    public void setDragBounds(int minX, int minY, int maxX, int maxY) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
    }

    @Override
    public void updateHoverState(int mouseX, int mouseY) {
        super.updateHoverState(mouseX, mouseY);

        if (collapsed)
            return;

        float scale = (float) width / initialWidth;
        int contentX = getContentX();
        int contentY = getContentY();
        int scaledMouseX = (int) ((mouseX - contentX) / scale + contentX);
        int scaledMouseY = (int) ((mouseY - contentY) / scale + contentY);

        for (Widget child : getChildren()) {
            if (child.isVisible()) {
                child.updateHoverState(scaledMouseX, scaledMouseY);
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible)
            return;

        int shadowOffset = dragging ? 8 : 4;
        int shadowColor = Theme.withAlpha(Theme.SHADOW, dragging ? 0.4f : 0.2f);
        graphics.fill(x + shadowOffset, y + shadowOffset, x + width + shadowOffset, y + height + shadowOffset,
                shadowColor);

        RenderHelper.renderSurface(graphics, x, y, width, height, Theme.BORDER_RADIUS, false);

        if (getTitle() != null && !getTitle().isEmpty()) {
            graphics.fill(x, y, x + width, y + TITLE_BAR_HEIGHT, Theme.withAlpha(Theme.SURFACE_LIGHT, 0.5f));

            int titleColor = dragging ? Theme.ACCENT : Theme.TEXT_PRIMARY;
            String arrow = collapsed ? "◀" : "▼";

            graphics.drawString(Minecraft.getInstance().font,
                    getTitle(), x + getPadding(), y + (TITLE_BAR_HEIGHT - 8) / 2, titleColor);

            int arrowWidth = Minecraft.getInstance().font.width(arrow);
            graphics.drawString(Minecraft.getInstance().font,
                    arrow, x + width - getPadding() - arrowWidth, y + (TITLE_BAR_HEIGHT - 8) / 2, titleColor);
        }

        if (collapsed)
            return;

        float scale = (float) width / initialWidth;

        graphics.pose().pushMatrix();

        int contentX = getContentX();
        int contentY = getContentY();
        graphics.pose().translate((float) contentX, (float) contentY);
        graphics.pose().scale(scale, scale);
        graphics.pose().translate((float) -contentX, (float) -contentY);

        for (Widget child : getChildren()) {
            if (child.isVisible()) {
                child.render(graphics, (int) ((mouseX - contentX) / scale + contentX),
                        (int) ((mouseY - contentY) / scale + contentY), partialTick);
            }
        }

        graphics.pose().popMatrix();

        if (resizing || (!collapsed && isOverResizeHandle(mouseX, mouseY) != ResizeHandle.NONE)) {
            int handleColor = resizing ? Theme.ACCENT : Theme.withAlpha(Theme.ACCENT, 0.5f);
            graphics.fill(x + width - RESIZE_HANDLE_SIZE, y + height - RESIZE_HANDLE_SIZE,
                    x + width, y + height, handleColor);
        }
    }

    @Override
    public void renderOverlay(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible || collapsed)
            return;

        float scale = (float) width / initialWidth;
        int contentX = getContentX();
        int contentY = getContentY();

        graphics.pose().pushMatrix();
        graphics.pose().translate((float) contentX, (float) contentY);
        graphics.pose().scale(scale, scale);
        graphics.pose().translate((float) -contentX, (float) -contentY);

        for (Widget child : getChildren()) {
            if (child.isVisible()) {
                child.renderOverlay(graphics, (int) ((mouseX - contentX) / scale + contentX),
                        (int) ((mouseY - contentY) / scale + contentY), partialTick);
            }
        }

        graphics.pose().popMatrix();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!enabled || !visible)
            return false;

        if (button == 0) {
            if (!collapsed) {
                ResizeHandle handle = isOverResizeHandle((int) mouseX, (int) mouseY);
                if (handle != ResizeHandle.NONE) {
                    resizing = true;
                    activeHandle = handle;
                    dragStartX = (int) mouseX;
                    dragStartY = (int) mouseY;
                    dragStartWidth = width;
                    return true;
                }
            }

            if (isOverTitleBar((int) mouseX, (int) mouseY)) {
                if (mouseX >= x + width - getPadding() - 15) {
                    setCollapsed(!collapsed);
                    if (onLayoutChange != null)
                        onLayoutChange.run();
                    return true;
                }

                dragging = true;
                dragStartX = (int) mouseX;
                dragStartY = (int) mouseY;
                dragStartCardX = x;
                dragStartCardY = y;
                return true;
            }
        }

        if (button == 1) {
            if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
                setCollapsed(!collapsed);
                if (onLayoutChange != null)
                    onLayoutChange.run();
                return true;
            }
        }

        if (collapsed)
            return false;

        float scale = (float) width / initialWidth;
        int contentX = getContentX();
        int contentY = getContentY();
        double scaledMouseX = (mouseX - contentX) / scale + contentX;
        double scaledMouseY = (mouseY - contentY) / scale + contentY;

        java.util.List<Widget> children = getChildren();
        for (Widget child : children) {
            if (child.isMouseOver(scaledMouseX, scaledMouseY)) {
                if (child.mouseClicked(scaledMouseX, scaledMouseY, button)) {
                    return true;
                }
            }
        }

        for (Widget child : children) {
            if (child.mouseClicked(scaledMouseX, scaledMouseY, button)) {
                return true;
            }
        }

        return isMouseOver(mouseX, mouseY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (dragging) {
                dragging = false;
                if (onLayoutChange != null)
                    onLayoutChange.run();
                return true;
            }
            if (resizing) {
                resizing = false;
                activeHandle = ResizeHandle.NONE;
                if (onLayoutChange != null)
                    onLayoutChange.run();
                return true;
            }
        }

        if (collapsed)
            return false;

        float scale = (float) width / initialWidth;
        int contentX = getContentX();
        int contentY = getContentY();
        double scaledMouseX = (mouseX - contentX) / scale + contentX;
        double scaledMouseY = (mouseY - contentY) / scale + contentY;

        java.util.List<Widget> children = getChildren();
        for (Widget child : children) {
            if (child.mouseReleased(scaledMouseX, scaledMouseY, button)) {
                return true;
            }
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    public void pack() {
        float scale = (float) width / initialWidth;
        int usedHeight = getContentY() - y;
        int childrenHeight = 0;
        for (Widget child : getChildren()) {
            if (child.isVisible()) {
                childrenHeight += child.getHeight() + Theme.SPACING_SMALL;
            }
        }
        if (childrenHeight > 0) {
            childrenHeight -= Theme.SPACING_SMALL;
        }
        int targetHeight = usedHeight + (int) (childrenHeight * scale) + getPadding();

        this.expandedHeight = targetHeight;
        if (!collapsed) {
            this.height = targetHeight;
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging) {
            int deltaX = (int) mouseX - dragStartX;
            int deltaY = (int) mouseY - dragStartY;

            int gridSize = 10;
            int targetX = dragStartCardX + deltaX;
            int targetY = dragStartCardY + deltaY;

            if (isShiftDown()) {
                targetX = Math.round((float) targetX / gridSize) * gridSize;
                targetY = Math.round((float) targetY / gridSize) * gridSize;
            }

            targetX = Math.max(minX, Math.min(maxX - width, targetX));
            targetY = Math.max(minY, Math.min(maxY - height, targetY));

            x = targetX;
            y = targetY;
            updateLayout();
            return true;
        }

        if (resizing) {
            int deltaX = (int) mouseX - dragStartX;

            if (activeHandle == ResizeHandle.BOTTOM_RIGHT || activeHandle == ResizeHandle.RIGHT) {
                int newWidth = dragStartWidth + deltaX;
                if (isShiftDown()) {
                    newWidth = Math.round((float) newWidth / 10) * 10;
                }
                width = Math.max(50, newWidth);
            }

            updateLayout();
            return true;
        }

        if (collapsed)
            return false;

        float scale = (float) width / initialWidth;
        int contentX = getContentX();
        int contentY = getContentY();
        double scaledMouseX = (mouseX - contentX) / scale + contentX;
        double scaledMouseY = (mouseY - contentY) / scale + contentY;

        double scaledDragX = dragX / scale;
        double scaledDragY = dragY / scale;

        java.util.List<Widget> children = getChildren();
        for (Widget child : children) {
            if (child.mouseDragged(scaledMouseX, scaledMouseY, button, scaledDragX, scaledDragY)) {
                return true;
            }
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private boolean isOverTitleBar(int mouseX, int mouseY) {
        if (getTitle() == null || getTitle().isEmpty())
            return false;
        return mouseX >= x && mouseX <= x + width &&
                mouseY >= y && mouseY <= y + TITLE_BAR_HEIGHT;
    }

    private ResizeHandle isOverResizeHandle(int mouseX, int mouseY) {
        if (mouseX >= x + width - RESIZE_HANDLE_SIZE && mouseX <= x + width &&
                mouseY >= y + height - RESIZE_HANDLE_SIZE && mouseY <= y + height) {
            return ResizeHandle.BOTTOM_RIGHT;
        }
        if (mouseX >= x + width - RESIZE_HANDLE_SIZE && mouseX <= x + width &&
                mouseY >= y && mouseY <= y + height) {
            return ResizeHandle.RIGHT;
        }
        return ResizeHandle.NONE;
    }

    public void updateLayout() {
        int contentX = getContentX();
        int contentY = getContentY();
        float scale = (float) width / initialWidth;
        int contentWidth = (int) ((width - getPadding() * 2) / scale);
        int currentY = contentY;

        for (Widget child : getChildren()) {
            child.setX(contentX);
            child.setY(currentY);
            child.setWidth(contentWidth);
            if (child.isVisible()) {
                currentY += child.getHeight() + Theme.SPACING_SMALL;
            }
        }
        pack();
    }

    @Override
    public int getContentY() {
        int contentY = y + getPadding();
        if (getTitle() != null && !getTitle().isEmpty()) {
            contentY += TITLE_BAR_HEIGHT;
        }
        return contentY;
    }

    @Override
    public int getContentHeight() {
        int usedHeight = getPadding();
        if (getTitle() != null && !getTitle().isEmpty()) {
            usedHeight += TITLE_BAR_HEIGHT;
        }
        return height - usedHeight - getPadding();
    }

    public boolean isDragging() {
        return dragging;
    }

    public int getInitialWidth() {
        return initialWidth;
    }

    public void setInitialWidth(int initialWidth) {
        this.initialWidth = initialWidth;
    }

    public boolean isResizing() {
        return resizing;
    }

    private boolean isShiftDown() {
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT) ||
                InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (collapsed)
            return false;

        float scale = (float) width / initialWidth;
        int contentX = getContentX();
        int contentY = getContentY();
        double scaledMouseX = (mouseX - contentX) / scale + contentX;
        double scaledMouseY = (mouseY - contentY) / scale + contentY;

        java.util.List<Widget> children = getChildren();
        for (Widget child : children) {
            if (child.mouseScrolled(scaledMouseX, scaledMouseY, scrollX, scrollY)) {
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
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

    public int getExpandedHeight() {
        return expandedHeight;
    }

    public void setExpandedHeight(int expandedHeight) {
        this.expandedHeight = expandedHeight;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (collapsed)
            return false;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        if (collapsed)
            return false;
        return super.charTyped(character, modifiers);
    }
}
