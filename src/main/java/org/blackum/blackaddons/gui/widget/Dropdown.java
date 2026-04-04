package org.blackum.blackaddons.gui.widget;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import org.blackum.blackaddons.gui.animation.Animation;
import org.blackum.blackaddons.gui.animation.Easing;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.render.RenderHelper;
import java.util.List;
import java.util.function.Consumer;

public class Dropdown extends Widget {

    private String label;
    private List<String> options;
    private int selectedIndex = -1;
    private boolean expanded = false;
    private Consumer<String> onSelect;
    private Runnable onExpand;

    private Animation hoverAnimation;
    private Animation expandAnimation;
    private double menuScrollOffset = 0;
    private static final int MAX_MENU_HEIGHT = 130;
    private static final int OPTION_HEIGHT = 25;
    private static final int MENU_GAP = 2;

    public Dropdown(int x, int y, int width, String label, List<String> options, Consumer<String> onSelect) {
        this(x, y, width, Theme.BUTTON_HEIGHT, label, options, onSelect);
    }

    public Dropdown(int x, int y, int width, int height, String label, List<String> options,
            Consumer<String> onSelect) {
        super(x, y, width, height);
        this.label = label;
        this.options = options;
        this.onSelect = onSelect;

        this.hoverAnimation = new Animation(0, 1, Theme.ANIM_HOVER, Easing::easeOut);
        this.expandAnimation = new Animation(0, 1, Theme.ANIM_CLICK, Easing::easeOutBack);
    }

    public void setOnExpand(Runnable onExpand) {
        this.onExpand = onExpand;
    }

    public void collapse() {
        expanded = false;
    }

    @Override
    public void onScrolled() {
        collapse();
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(int selectedIndex) {
        if (selectedIndex < -1 || selectedIndex >= options.size()) {
            return;
        }
        this.selectedIndex = selectedIndex;
    }

    public void setSelectedOption(String option) {
        if (option == null)
            return;
        for (int i = 0; i < options.size(); i++) {
            if (option.equalsIgnoreCase(options.get(i))) {
                setSelectedIndex(i);
                return;
            }
        }
    }

    public void setOptions(List<String> newOptions) {
        this.options = newOptions;
        if (selectedIndex >= options.size()) {
            selectedIndex = options.isEmpty() ? -1 : 0;
        }
    }

    public boolean isExpanded() {
        return expanded;
    }

    @Override
    public boolean hasActiveOverlay() {
        return visible && expanded;
    }

    private int getTotalMenuHeight() {
        return options.size() * OPTION_HEIGHT;
    }

    private int getMenuHeight() {
        return Math.min(getTotalMenuHeight(), MAX_MENU_HEIGHT);
    }

    private int getViewportHeight() {
        return (int) (Minecraft.getInstance().getWindow().getGuiScaledHeight() / RenderHelper.getGuiScaleFactor());
    }

    private boolean shouldOpenUpward() {
        int menuHeight = getMenuHeight();
        int spaceBelow = getViewportHeight() - (y + height + MENU_GAP);
        int spaceAbove = y - MENU_GAP;
        return spaceBelow < menuHeight && spaceAbove > spaceBelow;
    }

    private int getMenuBaseY() {
        return shouldOpenUpward() ? y - MENU_GAP - getMenuHeight() : y + height + MENU_GAP;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible)
            return;

        RenderHelper.renderSurface(graphics, x, y, width, height, Theme.BORDER_RADIUS_SMALL, false);

        int textColor = enabled ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY;
        String text = selectedIndex >= 0 && selectedIndex < options.size() ? options.get(selectedIndex) : label;

        if (Minecraft.getInstance().font.width(text) > width - 20) {
            text = Minecraft.getInstance().font.plainSubstrByWidth(text, width - 25) + "...";
        }

        int textY = y + (height - 8) / 2;
        graphics.text(Minecraft.getInstance().font, text, x + 10, textY, textColor);

        String arrow = expanded ? "▲" : "▼";
        int arrowWidth = Minecraft.getInstance().font.width(arrow);
        graphics.text(Minecraft.getInstance().font, arrow, x + width - 15 - arrowWidth / 2, textY,
                Theme.TEXT_SECONDARY);
    }

    @Override
    public void extractRenderOverlay(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int rawMouseX, int rawMouseY,
            float partialTick) {
        if (!visible || !expanded)
            return;

        int scrollOffset = mouseY - rawMouseY;
        int totalHeight = getTotalMenuHeight();
        int menuHeight = getMenuHeight();
        int menuY = getMenuBaseY() - scrollOffset;

        graphics.fill(x - 3, menuY - 3, x + width + 3, menuY + menuHeight + 3, 0xFF000000);
        RenderHelper.renderSurface(graphics, x, menuY, width, menuHeight, Theme.BORDER_RADIUS_SMALL, false);
        RenderHelper.renderRoundedOutline(graphics, x, menuY, width, menuHeight, Theme.BORDER_RADIUS_SMALL,
                Theme.withAlpha(Theme.BORDER, 0.5f));

        graphics.enableScissor(x, menuY, x + width, menuY + menuHeight);

        graphics.pose().pushMatrix();
        graphics.pose().translate(0f, (float) -menuScrollOffset);

        for (int i = 0; i < options.size(); i++) {
            String option = options.get(i);
            int optY = menuY + (i * OPTION_HEIGHT);

            boolean isOptHovered = mouseX >= x && mouseX <= x + width && mouseY >= optY + scrollOffset - menuScrollOffset
                    && mouseY < optY + scrollOffset + OPTION_HEIGHT - menuScrollOffset;

            if (isOptHovered) {
                graphics.fill(x + 2, optY, x + width - 2, optY + OPTION_HEIGHT,
                        Theme.withAlpha(Theme.GLASS_HIGHLIGHT, 0.2f));
            }

            if (i == selectedIndex) {
                graphics.text(Minecraft.getInstance().font, ChatFormatting.AQUA + option, x + 10,
                        optY + (OPTION_HEIGHT - 8) / 2, Theme.ACCENT);
            } else {
                graphics.text(Minecraft.getInstance().font, option, x + 10, optY + (OPTION_HEIGHT - 8) / 2,
                        Theme.TEXT_PRIMARY);
            }
        }

        graphics.pose().popMatrix();
        graphics.disableScissor();

        if (totalHeight > MAX_MENU_HEIGHT) {
            int scrollBarWidth = 2;
            int scrollBarX = x + width - 4;
            int scrollBarHeight = (int) ((menuHeight / (double) totalHeight) * menuHeight);
            double progress = menuScrollOffset / (totalHeight - menuHeight);
            int scrollBarY = (int) (menuY + progress * (menuHeight - scrollBarHeight));
            graphics.fill(scrollBarX, scrollBarY, scrollBarX + scrollBarWidth, scrollBarY + scrollBarHeight,
                    0x88FFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, MouseButtonEvent event) {
        if (!enabled || !visible)
            return false;

        int button = event.buttonInfo().button();

        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            expanded = !expanded;
            if (expanded && onExpand != null) {
                onExpand.run();
            }
            if (expanded)
                menuScrollOffset = 0;
            Minecraft.getInstance().getSoundManager()
                    .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }

        if (expanded) {
            int menuY = getMenuBaseY();
            int menuHeight = getMenuHeight();

            if (mouseX >= x && mouseX <= x + width && mouseY >= menuY && mouseY <= menuY + menuHeight) {
                int clickedIndex = (int) ((mouseY - menuY + menuScrollOffset) / OPTION_HEIGHT);
                if (clickedIndex >= 0 && clickedIndex < options.size()) {
                    selectedIndex = clickedIndex;
                    if (onSelect != null) {
                        onSelect.accept(options.get(clickedIndex));
                    }
                    expanded = false;
                    Minecraft.getInstance().getSoundManager()
                            .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    return true;
                }
            }
        }

        if (expanded) {
            expanded = false;
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, MouseButtonEvent event, double horizontalAmount,
            double verticalAmount) {
        if (expanded && isMouseOver(mouseX, mouseY)) {
            int totalHeight = getTotalMenuHeight();
            if (totalHeight > MAX_MENU_HEIGHT) {
                menuScrollOffset -= verticalAmount * 15;
                if (menuScrollOffset < 0)
                    menuScrollOffset = 0;
                if (menuScrollOffset > totalHeight - MAX_MENU_HEIGHT)
                    menuScrollOffset = totalHeight - MAX_MENU_HEIGHT;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (!visible)
            return false;
        boolean mainOver = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        if (mainOver)
            return true;

        if (expanded) {
            int menuHeight = getMenuHeight();
            int menuY = getMenuBaseY();
            return mouseX >= x && mouseX <= x + width && mouseY >= menuY && mouseY <= menuY + menuHeight;
        }
        return false;
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused) {
            expanded = false;
        }
    }

    @Override
    public void tick() {
        if (isMouseOver(
                Minecraft.getInstance().mouseHandler.xpos()
                        * ((double) Minecraft.getInstance().getWindow().getGuiScaledWidth()
                                / Minecraft.getInstance().getWindow().getScreenWidth()),
                Minecraft.getInstance().mouseHandler.ypos()
                        * ((double) Minecraft.getInstance().getWindow().getGuiScaledHeight()
                                / Minecraft.getInstance().getWindow().getScreenHeight()))) {
            if (hoverAnimation.getValue() < 1) {
                hoverAnimation = new Animation(hoverAnimation.getValue(), 1, Theme.ANIM_HOVER, Easing::easeOut);
                hoverAnimation.start();
            }
        } else {
            if (hoverAnimation.getValue() > 0) {
                hoverAnimation = new Animation(hoverAnimation.getValue(), 0, Theme.ANIM_HOVER, Easing::easeOut);
                hoverAnimation.start();
            }
        }
    }
}
