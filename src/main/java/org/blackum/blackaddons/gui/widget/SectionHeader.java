package org.blackum.blackaddons.gui.widget;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.render.RenderHelper;

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
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible)
            return;

        int headerColor = Theme.BACKGROUND_SECONDARY;
        RenderHelper.renderRoundedRect(graphics, x, y, width, height, Theme.BORDER_RADIUS_SMALL, headerColor);

        graphics.text(Minecraft.getInstance().font, title, x + 10, y + (height - 8) / 2, Theme.TEXT_PRIMARY);

        if (onToggle != null && bulkCheckbox == null) {
            String arrow = collapsed ? "▶" : "▼";
            graphics.text(Minecraft.getInstance().font, arrow, x + width - 15, y + (height - 8) / 2, Theme.TEXT_PRIMARY);
        }

        if (bulkCheckbox != null) {
            bulkCheckbox.setX(x + width - bulkCheckbox.getWidth() - 10);
            bulkCheckbox.setY(y + (height - bulkCheckbox.getHeight()) / 2);
            bulkCheckbox.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, MouseButtonEvent event) {
        if (!enabled || !visible)
            return false;

        if (bulkCheckbox != null && bulkCheckbox.mouseClicked(mouseX, mouseY, event)) {
            return true;
        }

        if (isMouseOver(mouseX, mouseY) && onToggle != null) {
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
