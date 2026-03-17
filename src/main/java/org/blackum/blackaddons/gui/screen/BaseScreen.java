package org.blackum.blackaddons.gui.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.widget.Widget;
import org.blackum.blackaddons.gui.render.RenderHelper;
import org.blackum.blackaddons.gui.notification.NotificationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.blackum.blackaddons.gui.widget.ResizableCard;
import org.blackum.blackaddons.core.config.ConfigManager;

@SuppressWarnings("all")
public abstract class BaseScreen extends Screen {
    protected final List<Widget> widgets = new ArrayList<>();
    protected final Map<String, ResizableCard> managedCards = new HashMap<>();
    private Widget focusedWidget = null;

    public static boolean showHitboxes = false;
    public static boolean showDebugOverlay = false;
    public static int overlayX = 5;
    public static int overlayY = 5;
    public static float overlayScale = 1.0f;

    protected boolean isMovingOverlay = false;

    protected int gridStartX;
    protected int gridStartY;
    protected int gridWidth;
    protected int gridColumns;
    protected int gridRowHeight;
    protected int gridGap;
    protected int currentGridColumn = 0;
    protected int currentGridRow = 0;

    protected int containerX;
    protected int containerY;
    protected int containerWidth;
    protected int containerHeight;

    protected double scrollOffset = 0;
    protected int contentHeight = 0;
    protected int baseContentHeight = 0;
    protected double maxScroll = 0;
    protected boolean canScroll = false;

    protected Screen parent;

    protected BaseScreen(Component title) {
        this(title, null);
    }

    protected BaseScreen(Component title, Screen parent) {
        super(title);
        this.parent = parent;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (parent != null) {
            Minecraft.getInstance().setScreen(parent);
        } else {
            super.onClose();
        }
    }

    protected void initGrid(int x, int y, int width, int columns, int rowHeight, int gap) {
        this.gridStartX = x;
        this.gridStartY = y;
        this.gridWidth = width;
        this.gridColumns = columns;
        this.gridRowHeight = rowHeight;
        this.gridGap = gap;
        this.currentGridColumn = 0;
        this.currentGridRow = 0;
    }

    protected void initStandardGrid(int x, int y, int width) {
        initGrid(x, y, width, Theme.GRID_COLUMNS, Theme.CARD_HEIGHT_SMALL, Theme.GRID_GAP);
    }

    protected <T extends Widget> T addToGrid(T widget, int colSpan) {
        placeInGrid(widget, colSpan);
        widgets.add(widget);
        return widget;
    }

    protected <T extends Widget> T placeInGrid(T widget, int colSpan) {
        if (currentGridColumn + colSpan > gridColumns) {
            currentGridColumn = 0;
            currentGridRow++;
        }

        int cellWidth = (gridWidth - (gridColumns - 1) * gridGap) / gridColumns;
        int widgetWidth = cellWidth * colSpan + (colSpan - 1) * gridGap;

        int widgetX = gridStartX + currentGridColumn * (cellWidth + gridGap);
        int widgetY = gridStartY + currentGridRow * (gridRowHeight + gridGap);

        widget.setX(widgetX);
        widget.setY(widgetY);
        widget.setWidth(widgetWidth);

        currentGridColumn += colSpan;
        if (currentGridColumn >= gridColumns) {
            currentGridColumn = 0;
            currentGridRow++;
        }

        return widget;
    }

    protected float getGuiScaleFactor() {
        int forcedScale = ConfigManager.data.forcedGuiScale;
        if (forcedScale <= 0) return 1.0f;
        
        int vanillaScale = (int)Minecraft.getInstance().getWindow().getGuiScale();
        if (forcedScale >= vanillaScale) return 1.0f;
        
        return (float)forcedScale / vanillaScale;
    }

    @Override
    public void init() {
        super.init();
        
        float scale = getGuiScaleFactor();
        int virtualWidth = (int)(this.width / scale);
        int virtualHeight = (int)(this.height / scale);

        this.containerWidth = (int) (virtualWidth * 0.8);
        this.containerHeight = (int) (virtualHeight * 0.8);
        this.containerX = (virtualWidth - this.containerWidth) / 2;
        this.containerY = (virtualHeight - this.containerHeight) / 2;

        widgets.clear();
        isMovingOverlay = false;
        initWidgets();

        int maxWidgetY = 0;
        for (Widget w : widgets) {
            int relativeBottom = (w.getY() + w.getHeight()) - this.containerY;
            if (relativeBottom > maxWidgetY) {
                maxWidgetY = relativeBottom;
            }
        }
        this.baseContentHeight = Math.max(0, maxWidgetY + 20);
        this.contentHeight = this.baseContentHeight;
    }

    protected abstract void initWidgets();

    protected <T extends Widget> T addWidget(T widget) {
        widgets.add(widget);
        return widget;
    }

    public List<Widget> getWidgets() {
        return widgets;
    }

    protected int getContentHeight() {
        return this.baseContentHeight;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        float scale = getGuiScaleFactor();
        
        int scaledMouseX = (int)(mouseX / scale);
        int scaledMouseY = (int)(mouseY / scale);

        graphics.enableScissor(
            (int)(containerX * scale), 
            (int)(containerY * scale), 
            (int)((containerX + containerWidth) * scale), 
            (int)((containerY + containerHeight) * scale)
        );

        graphics.pose().pushMatrix();
        graphics.pose().scale(scale, scale);

        this.contentHeight = getContentHeight();

        RenderHelper.renderSurface(
                graphics, containerX, containerY, containerWidth, containerHeight,
                Theme.BORDER_RADIUS_LARGE, false);

        maxScroll = Math.max(0, contentHeight - (containerHeight - 40));

        if (maxScroll < 32)
            maxScroll = 0;

        canScroll = maxScroll > 0;

        if (scrollOffset < 0)
            scrollOffset = 0;
        if (scrollOffset > maxScroll)
            scrollOffset = maxScroll;

        graphics.pose().pushMatrix();
        graphics.pose().translate(0f, (float) -scrollOffset);

        renderScrolledContent(graphics, scaledMouseX, (int) (scaledMouseY + scrollOffset), partialTick);

        boolean mouseCaptured = false;
        for (int i = widgets.size() - 1; i >= 0; i--) {
            Widget widget = widgets.get(i);
            if (widget.isVisible()) {
                if (!mouseCaptured && widget.isMouseOver(scaledMouseX, scaledMouseY + scrollOffset)) {
                    widget.updateHoverState(scaledMouseX, (int) (scaledMouseY + scrollOffset));
                    mouseCaptured = true;
                } else {
                    widget.updateHoverState(-1, -1);
                }
            }
        }

        for (Widget widget : widgets) {
            if (widget.isVisible()) {
                widget.render(graphics, scaledMouseX, (int) (scaledMouseY + scrollOffset), partialTick);

                if (showHitboxes) {
                    graphics.fill(widget.getX(), widget.getY(), widget.getX() + widget.getWidth(), widget.getY() + 1,
                            0xFFFF0000); // Top
                    graphics.fill(widget.getX(), widget.getY() + widget.getHeight() - 1,
                            widget.getX() + widget.getWidth(), widget.getY() + widget.getHeight(), 0xFFFF0000); // Bottom
                    graphics.fill(widget.getX(), widget.getY(), widget.getX() + 1, widget.getY() + widget.getHeight(),
                            0xFFFF0000); // Left
                    graphics.fill(widget.getX() + widget.getWidth() - 1, widget.getY(),
                            widget.getX() + widget.getWidth(), widget.getY() + widget.getHeight(), 0xFFFF0000); // Right
                }
            }
        }

        graphics.pose().popMatrix();
        graphics.disableScissor();

        for (Widget widget : widgets) {
            if (widget.isVisible()) {
                widget.renderOverlay(graphics, scaledMouseX, (int) (scaledMouseY + scrollOffset), scaledMouseX, scaledMouseY, partialTick);
            }
        }

        if (canScroll) {
            int scrollBarHeight = (int) ((containerHeight / (double) contentHeight) * containerHeight);
            if (scrollBarHeight < 30)
                scrollBarHeight = 30;

            double progress = scrollOffset / maxScroll;
            int scrollBarY = (int) (containerY + (progress * (containerHeight - scrollBarHeight)));
            int scrollBarX = containerX + containerWidth - 6;

            // Track
            graphics.fill(scrollBarX, containerY, scrollBarX + 4, containerY + containerHeight, Theme.SCROLLBAR_BG);

            // Thumb
            graphics.fill(scrollBarX, scrollBarY, scrollBarX + 4, scrollBarY + scrollBarHeight, Theme.SCROLLBAR_THUMB);
        }

        renderTooltips(graphics, scaledMouseX, scaledMouseY);

        NotificationManager.getInstance().render(graphics);
        
        graphics.pose().popMatrix();
    }

    protected void renderScrolledContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    protected void renderTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override
    public void tick() {
        super.tick();
        for (Widget widget : widgets) {
            if (widget.isVisible()) {
                widget.tick();
            }
        }
    }

    private boolean isDraggingScrollbar = false;

    protected boolean showClickDebug = false;

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean pressed) {
        float scale = getGuiScaleFactor();
        Minecraft mc = Minecraft.getInstance();
        double windowWidth = mc.getWindow().getScreenWidth();
        double windowHeight = mc.getWindow().getScreenHeight();
        double scaledWidth = this.width / scale;
        double scaledHeight = this.height / scale;

        double mouseX = (mc.mouseHandler.xpos() * (this.width / windowWidth)) / scale;
        double rawMouseY = (mc.mouseHandler.ypos() * (this.height / windowHeight)) / scale;
        double mouseY = rawMouseY + scrollOffset;
        int button = event.button();

        if (showClickDebug && mc.player != null) {
            String msg = String.format("%s[Click] Scaled: %.1f,%.1f (Raw: %.1f,%.1f, Scale: %.2f)", 
                    ChatFormatting.YELLOW, mouseX, mouseY, mc.mouseHandler.xpos(), mc.mouseHandler.ypos(), scale);
            mc.player.displayClientMessage(Component.literal(msg), false);
        }

        if (canScroll) {
            int scrollBarX = containerX + containerWidth - 6;
            if (mouseX >= scrollBarX && mouseX <= scrollBarX + 4 &&
                    rawMouseY >= containerY && rawMouseY <= containerY + containerHeight) {
                isDraggingScrollbar = true;
                return true;
            }
        }

        for (int i = widgets.size() - 1; i >= 0; i--) {
            Widget widget = widgets.get(i);
            if (widget.isVisible() && widget.isEnabled() && widget.isMouseOver(mouseX, mouseY)) {
                if (widget.mouseClicked(mouseX, mouseY, button)) {
                    setFocusedWidget(widget);
                    return true;
                }
            }
        }

        setFocusedWidget(null);
        return super.mouseClicked(event, pressed);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        isDraggingScrollbar = false;
        float scale = getGuiScaleFactor();
        Minecraft mc = Minecraft.getInstance();
        double mouseX = (mc.mouseHandler.xpos() * ((double) this.width / mc.getWindow().getScreenWidth())) / scale;
        double rawMouseY = (mc.mouseHandler.ypos() * ((double) this.height / mc.getWindow().getScreenHeight())) / scale;
        double mouseY = rawMouseY + scrollOffset;
        int button = event.button();

        for (int i = widgets.size() - 1; i >= 0; i--) {
            Widget widget = widgets.get(i);
            if (widget.isVisible() && widget.isEnabled()) {
                if (widget.mouseReleased(mouseX, mouseY, button)) {
                    return true;
                }
            }
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        float scale = getGuiScaleFactor();
        Minecraft mc = Minecraft.getInstance();
        double mouseX = (mc.mouseHandler.xpos() * ((double) this.width / mc.getWindow().getScreenWidth())) / scale;
        double rawMouseY = (mc.mouseHandler.ypos() * ((double) this.height / mc.getWindow().getScreenHeight())) / scale;
        double mouseY = rawMouseY + scrollOffset;
        int button = event.button();

        if (isDraggingScrollbar && canScroll) {
            int scrollBarHeight = (int) ((containerHeight / (double) contentHeight) * containerHeight);
            if (scrollBarHeight < 30)
                scrollBarHeight = 30;

            double trackHeight = containerHeight - scrollBarHeight;
            double movement = (dragY / scale) * ((double) maxScroll / trackHeight);

            scrollOffset += movement;
            if (scrollOffset < 0)
                scrollOffset = 0;
            if (scrollOffset > maxScroll)
                scrollOffset = maxScroll;
            notifyWidgetsScrolled();
            return true;
        }

        if (getFocusedWidget() != null && getFocusedWidget().mouseDragged(mouseX, mouseY, button, dragX / scale, dragY / scale)) {
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        float scale = getGuiScaleFactor();
        double scaledMouseX = mouseX / scale;
        double scaledMouseY = mouseY / scale;

        for (int i = widgets.size() - 1; i >= 0; i--) {
            Widget widget = widgets.get(i);
            if (widget.isVisible() && widget.isEnabled() && widget.isMouseOver(scaledMouseX, scaledMouseY + scrollOffset)) {
                if (widget.mouseScrolled(scaledMouseX, scaledMouseY + scrollOffset, scrollX, scrollY)) {
                    return true;
                }
            }
        }

        if (canScroll) {
            scrollOffset -= scrollY * 20;
            if (scrollOffset < 0)
                scrollOffset = 0;
            if (scrollOffset > maxScroll)
                scrollOffset = maxScroll;
            notifyWidgetsScrolled();
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (getFocusedWidget() != null && getFocusedWidget().isVisible() && getFocusedWidget().isEnabled()) {
            if (getFocusedWidget().keyPressed(event.key(), event.scancode(), event.modifiers())) {
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (getFocusedWidget() != null && getFocusedWidget().isVisible() && getFocusedWidget().isEnabled()) {
            char character = (char) event.codepoint();
            if (getFocusedWidget().charTyped(character, event.modifiers())) {
                return true;
            }
        }
        return super.charTyped(event);
    }

    protected void setFocusedWidget(Widget widget) {
        if (focusedWidget == widget)
            return;
        if (focusedWidget != null) {
            focusedWidget.setFocused(false);
        }
        focusedWidget = widget;
        if (focusedWidget != null) {
            focusedWidget.setFocused(true);
        }
    }

    protected void notifyWidgetsScrolled() {
        for (Widget widget : widgets) {
            if (widget.isVisible()) {
                widget.onScrolled();
            }
        }
    }

    protected Widget getFocusedWidget() {
        return focusedWidget;
    }

    public ResizableCard createResizableCard(String id, int defaultX, int defaultY, int defaultW, int defaultH,
            String title) {
        ConfigManager.CardState state = ConfigManager.data.lastLoadedCardStates.get(id);
        ResizableCard card;
        if (state != null) {
            card = new ResizableCard(state.x, state.y, state.width, state.height, title);
            card.setCollapsed(state.collapsed);
            if (state.initialWidth > 0) {
                card.setInitialWidth(state.initialWidth);
            }
            if (state.expandedHeight > 0) {
                card.setExpandedHeight(state.expandedHeight);
            }
        } else {
            card = new ResizableCard(defaultX, defaultY, defaultW, defaultH, title);
        }
        card.setOnLayoutChange(this::saveCardLayout);
        managedCards.put(id, card);
        return card;
    }

    public void saveCardLayout() {
        Map<String, ConfigManager.CardState> states = new HashMap<>();
        for (Map.Entry<String, ResizableCard> entry : managedCards.entrySet()) {
            ResizableCard card = entry.getValue();
            if (card != null) {
                states.put(entry.getKey(), new ConfigManager.CardState(
                        card.getX(), card.getY(), card.getWidth(), card.getHeight(),
                        card.isCollapsed(), card.getInitialWidth(), card.getExpandedHeight()));
            }
        }
        ConfigManager.data.lastLoadedCardStates.putAll(states);
        ConfigManager.data.cardStates = ConfigManager.data.lastLoadedCardStates;
        ConfigManager.save();
    }

    public void resetCardStates(String... ids) {
        for (String id : ids) {
            ConfigManager.data.lastLoadedCardStates.remove(id);
            managedCards.remove(id);
        }
        ConfigManager.data.cardStates = ConfigManager.data.lastLoadedCardStates;
        ConfigManager.save();
        this.init();
    }
}
