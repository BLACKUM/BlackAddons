package org.blackum.blackaddons.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.blackum.blackaddons.gui.render.Theme;

import java.util.function.Consumer;

public class SearchField extends TextField {

    private Consumer<String> onSearch;
    private String lastText = "";

    public SearchField(int x, int y, int width, Consumer<String> onSearch) {
        super(x, y, width, "");
        this.onSearch = onSearch;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        if (getText().isEmpty() && !focused) {
            int iconX = x + Theme.PADDING_SMALL;
            int iconY = y + (height - 8) / 2;
            int iconColor = Theme.withAlpha(Theme.TEXT_SECONDARY, 0.6f);
            graphics.text(Minecraft.getInstance().font, "🔍 Search...", iconX, iconY, iconColor);
        }

        if (!getText().isEmpty()) {
            int clearX = x + width - Theme.PADDING_SMALL - 8;
            int clearY = y + (height - 8) / 2;
            int clearColor = Theme.withAlpha(Theme.TEXT_SECONDARY, hovered ? 1.0f : 0.6f);
            graphics.text(Minecraft.getInstance().font, "✕", clearX, clearY, clearColor);
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (!getText().equals(lastText)) {
            lastText = getText();
            if (onSearch != null) {
                onSearch.accept(lastText);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, MouseButtonEvent event) {
        if (!enabled || !visible)
            return false;

        if (!getText().isEmpty()) {
            int clearX = x + width - Theme.PADDING_SMALL - 8;
            int clearY = y;
            int clearSize = 16;

            if (mouseX >= clearX && mouseX <= clearX + clearSize &&
                    mouseY >= clearY && mouseY <= clearY + height) {
                setText("");
                if (onSearch != null) {
                    onSearch.accept("");
                }
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, event);
    }

    public void setOnSearch(Consumer<String> onSearch) {
        this.onSearch = onSearch;
    }
}
