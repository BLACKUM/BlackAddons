package org.blackum.blackaddons.gui.widget;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.render.RenderHelper;

public class StatBox extends Widget {
    private String label;
    private String value;

    public StatBox(int x, int y, int width, String label, String value) {
        super(x, y, width, 50);
        this.label = label;
        this.value = value;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible)
            return;

        RenderHelper.renderRoundedRect(graphics, x, y, width, height, Theme.BORDER_RADIUS, Theme.BACKGROUND_SECONDARY);

        Minecraft mc = Minecraft.getInstance();
        graphics.centeredText(mc.font, label, x + width / 2, y + 10, Theme.ACCENT);
        graphics.centeredText(mc.font, ChatFormatting.WHITE + value, x + width / 2, y + 25, 0xFFFFFFFF);
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
