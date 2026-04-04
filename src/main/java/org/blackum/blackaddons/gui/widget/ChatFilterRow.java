package org.blackum.blackaddons.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.gui.render.RenderHelper;
import org.blackum.blackaddons.gui.render.Theme;

public class ChatFilterRow extends Widget {

    private static final int ROW_HEIGHT = 58;

    private final ConfigManager.ChatVisualFilter filter;
    private final Button toggleButton;
    private final Button deleteButton;

    public ChatFilterRow(ConfigManager.ChatVisualFilter filter, Runnable onToggled, Runnable onDelete) {
        super(0, 0, 0, ROW_HEIGHT);
        this.filter = filter;
        this.toggleButton = new Button(0, 0, 64, Theme.BUTTON_HEIGHT, "", () -> {
            filter.enabled = !filter.enabled;
            ConfigManager.save();
            onToggled.run();
        });
        this.deleteButton = new Button(0, 0, 70, Theme.BUTTON_HEIGHT, "Delete", onDelete);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible) {
            return;
        }

        RenderHelper.renderSurface(graphics, x, y, width, height, Theme.BORDER_RADIUS, false);

        String pattern = filter.pattern == null || filter.pattern.isBlank() ? "<empty>" : filter.pattern;
        int maxPatternWidth = Math.max(60, width - 170);
        if (Minecraft.getInstance().font.width(pattern) > maxPatternWidth) {
            pattern = Minecraft.getInstance().font.plainSubstrByWidth(pattern, maxPatternWidth - 6) + "...";
        }

        graphics.text(Minecraft.getInstance().font, pattern, x + 10, y + 10, Theme.TEXT_PRIMARY);
        graphics.text(Minecraft.getInstance().font, describeFilter(), x + 10, y + 28, Theme.TEXT_SECONDARY);

        toggleButton.setText(filter.enabled ? "Enabled" : "Disabled");
        toggleButton.setX(x + width - deleteButton.getWidth() - toggleButton.getWidth() - 16);
        toggleButton.setY(y + (height - toggleButton.getHeight()) / 2);
        deleteButton.setX(x + width - deleteButton.getWidth() - 8);
        deleteButton.setY(y + (height - deleteButton.getHeight()) / 2);

        toggleButton.extractRenderState(graphics, mouseX, mouseY, partialTick);
        deleteButton.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void updateHoverState(int mouseX, int mouseY) {
        super.updateHoverState(mouseX, mouseY);
        toggleButton.updateHoverState(mouseX, mouseY);
        deleteButton.updateHoverState(mouseX, mouseY);
    }

    @Override
    public void tick() {
        toggleButton.tick();
        deleteButton.tick();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, MouseButtonEvent event) {
        return toggleButton.mouseClicked(mouseX, mouseY, event) || deleteButton.mouseClicked(mouseX, mouseY, event);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, MouseButtonEvent event) {
        return toggleButton.mouseReleased(mouseX, mouseY, event)
                || deleteButton.mouseReleased(mouseX, mouseY, event);
    }

    private String describeFilter() {
        String matchType = switch (filter.matchType) {
            case STARTS_WITH -> "Starts with";
            case EXACT -> "Exact match";
            case REGEX -> "Regex";
            case CONTAINS -> "Contains";
        };

        return matchType + " | " + (filter.caseSensitive ? "Case-sensitive" : "Ignore case");
    }
}
