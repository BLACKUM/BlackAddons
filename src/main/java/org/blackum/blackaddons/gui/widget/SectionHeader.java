package org.blackum.blackaddons.gui.widget;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.blackum.blackaddons.gui.render.Theme;

public class SectionHeader extends Widget {
    private final String title;
    private Runnable onToggle;
    private Checkbox bulkCheckbox;
    private boolean collapsed;

    public SectionHeader(int width, String title) {
        super(0, 0, width, 25);
        this.title = ChatFormatting.BOLD + title;
    }

    public SectionHeader(int width, String title, boolean collapsed, Runnable onToggle) {
        this(width, title);
        this.collapsed = collapsed;
        this.onToggle = onToggle;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible)
            return;

        int currentX = x;
        if (onToggle != null) {
            String arrow = collapsed ? "▶ " : "▼ ";
            graphics.drawString(Minecraft.getInstance().font, arrow, currentX, y + 8, Theme.TEXT_SECONDARY);
            currentX += 12;
        }

        if (bulkCheckbox != null) {
            bulkCheckbox.setX(currentX);
            bulkCheckbox.setY(y + 4);
            bulkCheckbox.render(graphics, mouseX, mouseY, partialTick);
            currentX += bulkCheckbox.getWidth() + 8;
        }

        graphics.drawString(Minecraft.getInstance().font, title, currentX, y + 8, Theme.ACCENT);

        int titleWidth = Minecraft.getInstance().font.width(title);
        int lineX = currentX + titleWidth + 10;
        int lineW = width - (lineX - x);
        if (lineW > 0) {
            int centerY = y + 8 + 4;
            graphics.fill(lineX, centerY, x + width, centerY + 1, Theme.withAlpha(Theme.TEXT_SECONDARY, 0.3f));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !enabled)
            return false;

        if (bulkCheckbox != null && bulkCheckbox.isMouseOver(mouseX, mouseY)) {
            return bulkCheckbox.mouseClicked(mouseX, mouseY, button);
        }

        if (isMouseOver(mouseX, mouseY) && onToggle != null && button == 0) {
            collapsed = !collapsed;
            onToggle.run();
            return true;
        }
        return false;
    }

    @Override
    public void updateHoverState(int mouseX, int mouseY) {
        super.updateHoverState(mouseX, mouseY);
        if (bulkCheckbox != null) {
            bulkCheckbox.updateHoverState(mouseX, mouseY);
        }
    }

    @Override
    public void tick() {
        if (bulkCheckbox != null) {
            bulkCheckbox.tick();
        }
    }

    public void setBulkCheckbox(Checkbox checkbox) {
        this.bulkCheckbox = checkbox;
    }

    public boolean isCollapsed() {
        return collapsed;
    }

    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
    }

    public void setToggleCallback(Runnable onToggle) {
        this.onToggle = onToggle;
    }
}
