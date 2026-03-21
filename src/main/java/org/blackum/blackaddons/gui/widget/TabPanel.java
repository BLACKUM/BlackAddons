package org.blackum.blackaddons.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.blackum.blackaddons.gui.animation.Animation;
import org.blackum.blackaddons.gui.animation.Easing;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.render.RenderHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class TabPanel extends Widget {

    public static class Tab {
        private final TabPanel parent;
        public final String name;
        public final List<Widget> widgets = new ArrayList<>();

        public Tab(TabPanel parent, String name) {
            this.parent = parent;
            this.name = name;
        }

        public Tab addWidget(Widget widget) {
            widgets.add(widget);
            return this;
        }

        public TabPanel getParent() {
            return parent;
        }
    }

    private List<Tab> tabs = new ArrayList<>();
    private int selectedTabIndex = 0;
    private Consumer<Integer> onTabChange;

    private int tabWidth = 120;
    private int tabHeight = 40;
    private int contentPadding = 10;

    private Map<Integer, Animation> tabHoverAnimations = new HashMap<>();
    private Animation selectionAnimation;
    private double tabScrollOffset = 0;

    public TabPanel(int x, int y, int width, int height) {
        super(x, y, width, height);
        this.selectionAnimation = new Animation(0, 0, Theme.ANIM_NORMAL, Easing::easeOut);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (super.isMouseOver(mouseX, mouseY))
            return true;

        if (selectedTabIndex >= 0 && selectedTabIndex < tabs.size()) {
            Tab currentTab = tabs.get(selectedTabIndex);
            for (Widget widget : currentTab.widgets) {
                if (widget.isVisible() && widget.isMouseOver(mouseX, mouseY)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void updateHoverState(int mouseX, int mouseY) {
        super.updateHoverState(mouseX, mouseY);

        if (selectedTabIndex >= 0 && selectedTabIndex < tabs.size()) {
            Tab currentTab = tabs.get(selectedTabIndex);
            for (Widget widget : currentTab.widgets) {
                if (widget.isVisible()) {
                    widget.updateHoverState(mouseX, mouseY);
                }
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible)
            return;

        renderTabs(graphics, mouseX, mouseY);
        renderContent(graphics, mouseX, mouseY, partialTick);
    }

    private void renderTabs(GuiGraphics graphics, int mouseX, int mouseY) {
        int tabX = x;
        int tabY = y;

        graphics.enableScissor(tabX, tabY, tabX + tabWidth, tabY + height);
        graphics.pose().pushMatrix();
        graphics.pose().translate(0f, (float) -tabScrollOffset);

        float selectionY = tabY + selectionAnimation.getValue() * tabHeight;
        int selectionColor = Theme.withAlpha(Theme.ACCENT, 0.2f);
        RenderHelper.renderRoundedRect(graphics, tabX, (int) selectionY, tabWidth, tabHeight,
                Theme.BORDER_RADIUS_SMALL, selectionColor);

        int currentY = tabY;
        for (int i = 0; i < tabs.size(); i++) {
            Tab tab = tabs.get(i);
            boolean isSelected = i == selectedTabIndex;
            boolean isHovered = mouseX >= tabX && mouseX <= tabX + tabWidth &&
                    mouseY >= currentY - tabScrollOffset && mouseY <= currentY + tabHeight - tabScrollOffset;

            Animation hoverAnim = tabHoverAnimations.computeIfAbsent(i,
                    k -> new Animation(0, 0, Theme.ANIM_HOVER, Easing::easeOut));

            if (isHovered && !isSelected) {
                int hoverColor = Theme.withAlpha(Theme.SURFACE_LIGHT, 0.5f);
                graphics.fill(tabX, currentY, tabX + tabWidth, currentY + tabHeight, hoverColor);
            }

            int textColor = isSelected ? Theme.ACCENT : Theme.TEXT_SECONDARY;
            if (!isSelected && hoverAnim.getValue() > 0) {
                textColor = Theme.TEXT_PRIMARY;
            }

            int textX = tabX + (tabWidth - Minecraft.getInstance().font.width(tab.name)) / 2;
            int textY = currentY + (tabHeight - 8) / 2;
            graphics.drawString(Minecraft.getInstance().font, tab.name, textX, textY, textColor);

            if (isSelected) {
                int lineX = tabX + tabWidth - 3;
                graphics.fill(lineX, currentY + 5, lineX + 3, currentY + tabHeight - 5, Theme.ACCENT);
            }

            currentY += tabHeight;
        }

        graphics.pose().popMatrix();
        graphics.disableScissor();
    }

    private void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (selectedTabIndex < 0 || selectedTabIndex >= tabs.size())
            return;

        Tab currentTab = tabs.get(selectedTabIndex);
        for (Widget widget : currentTab.widgets) {
            if (widget.isVisible()) {
                widget.render(graphics, mouseX, mouseY, partialTick);
            }
        }
    }

    @Override
    public void renderOverlay(GuiGraphics graphics, int mouseX, int mouseY, int rawMouseX, int rawMouseY,
            float partialTick) {
        if (!visible)
            return;

        if (selectedTabIndex >= 0 && selectedTabIndex < tabs.size()) {
            Tab currentTab = tabs.get(selectedTabIndex);
            for (Widget widget : currentTab.widgets) {
                if (widget.isVisible()) {
                    widget.renderOverlay(graphics, mouseX, mouseY, rawMouseX, rawMouseY, partialTick);
                }
            }
        }
    }

    public int getMaxContentHeight() {
        if (selectedTabIndex < 0 || selectedTabIndex >= tabs.size())
            return height;

        Tab currentTab = tabs.get(selectedTabIndex);
        int maxY = 0;
        int contentY = y;

        for (Widget widget : currentTab.widgets) {
            if (widget.isVisible()) {
                int relativeY = widget.getY() - contentY;
                int widgetBottom = relativeY + widget.getHeight();
                if (widgetBottom > maxY) {
                    maxY = widgetBottom;
                }
            }
        }
        return Math.max(height, maxY + 5);
    }

    @Override
    public void tick() {
        for (int i = 0; i < tabs.size(); i++) {
            Animation hoverAnim = tabHoverAnimations.get(i);
            if (hoverAnim != null && hoverAnim.isRunning()) {
            }
        }

        if (selectedTabIndex >= 0 && selectedTabIndex < tabs.size()) {
            Tab currentTab = tabs.get(selectedTabIndex);
            for (Widget widget : currentTab.widgets) {
                widget.tick();
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!enabled || !visible)
            return false;

        int tabX = x;
        int currentY = y;

        for (int i = 0; i < tabs.size(); i++) {
            if (mouseX >= tabX && mouseX <= tabX + tabWidth &&
                    mouseY >= currentY - tabScrollOffset && mouseY <= currentY + tabHeight - tabScrollOffset) {
                selectTab(i);
                return true;
            }
            currentY += tabHeight;
        }

        if (selectedTabIndex >= 0 && selectedTabIndex < tabs.size()) {
            Tab currentTab = tabs.get(selectedTabIndex);
            java.util.List<Widget> tabWidgets = currentTab.widgets;
            for (int i = tabWidgets.size() - 1; i >= 0; i--) {
                Widget widget = tabWidgets.get(i);
                if (widget.isVisible() && widget.hasActiveOverlay() && widget.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            }
            for (int i = tabWidgets.size() - 1; i >= 0; i--) {
                Widget widget = tabWidgets.get(i);
                if (widget.isVisible() && widget.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (selectedTabIndex >= 0 && selectedTabIndex < tabs.size()) {
            Tab currentTab = tabs.get(selectedTabIndex);
            for (Widget widget : currentTab.widgets) {
                if (widget.mouseReleased(mouseX, mouseY, button)) {
                    return true;
                }
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (selectedTabIndex >= 0 && selectedTabIndex < tabs.size()) {
            Tab currentTab = tabs.get(selectedTabIndex);
            for (Widget widget : currentTab.widgets) {
                if (widget.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
                    return true;
                }
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= x && mouseX <= x + tabWidth && mouseY >= y && mouseY <= y + height) {
            double maxTabScroll = Math.max(0, tabs.size() * tabHeight - height);
            tabScrollOffset -= scrollY * 20;
            if (tabScrollOffset < 0)
                tabScrollOffset = 0;
            if (tabScrollOffset > maxTabScroll)
                tabScrollOffset = maxTabScroll;
            return true;
        }

        if (selectedTabIndex >= 0 && selectedTabIndex < tabs.size()) {
            Tab currentTab = tabs.get(selectedTabIndex);
            for (int i = currentTab.widgets.size() - 1; i >= 0; i--) {
                Widget widget = currentTab.widgets.get(i);
                if (widget.isVisible() && widget.hasActiveOverlay()
                        && widget.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
                    return true;
                }
            }
            for (Widget widget : currentTab.widgets) {
                if (widget.isVisible() && widget.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
                    return true;
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (selectedTabIndex >= 0 && selectedTabIndex < tabs.size()) {
            Tab currentTab = tabs.get(selectedTabIndex);
            for (Widget widget : currentTab.widgets) {
                if (widget.keyPressed(keyCode, scanCode, modifiers)) {
                    return true;
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        if (selectedTabIndex >= 0 && selectedTabIndex < tabs.size()) {
            Tab currentTab = tabs.get(selectedTabIndex);
            for (Widget widget : currentTab.widgets) {
                if (widget.charTyped(character, modifiers)) {
                    return true;
                }
            }
        }
        return super.charTyped(character, modifiers);
    }

    public void selectTab(int index) {
        if (index >= 0 && index < tabs.size() && index != selectedTabIndex) {
            selectedTabIndex = index;

            selectionAnimation = new Animation(selectionAnimation.getValue(), (float) index, Theme.ANIM_NORMAL,
                    Easing::easeOut);
            selectionAnimation.start();

            if (onTabChange != null) {
                onTabChange.accept(index);
            }
        }
    }

    public void selectTabByName(String name) {
        for (int i = 0; i < tabs.size(); i++) {
            if (tabs.get(i).name.equalsIgnoreCase(name)) {
                selectTab(i);
                return;
            }
        }
    }

    public Tab addTab(String name) {
        Tab tab = new Tab(this, name);
        tabs.add(tab);
        return tab;
    }

    public Tab getTab(int index) {
        return tabs.get(index);
    }

    public int getSelectedTabIndex() {
        return selectedTabIndex;
    }

    public void setOnTabChange(Consumer<Integer> onTabChange) {
        this.onTabChange = onTabChange;
    }

    public int getContentX() {
        return x + tabWidth + contentPadding;
    }

    public int getContentY() {
        return y;
    }

    public int getContentWidth() {
        return width - tabWidth - contentPadding;
    }

    public int getContentHeight() {
        return height;
    }
}
