package org.blackum.blackaddons.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.blackum.blackaddons.gui.render.Theme;

public class TextWidget extends Widget {
    private String text;
    private int color = Theme.TEXT;
    private boolean centered = false;

    public TextWidget(int x, int y, String text) {
        this(x, y, text, Theme.TEXT, false);
    }

    public TextWidget(int x, int y, String text, int color, boolean centered) {
        super(x, y, Minecraft.getInstance().font.width(text), 10);
        this.text = text;
        this.color = color;
        this.centered = centered;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible)
            return;
        if (centered) {
            graphics.centeredText(Minecraft.getInstance().font, text, x + width / 2, y, color);
        } else {
            graphics.text(Minecraft.getInstance().font, text, x, y, color);
        }
    }

    public void setText(String text) {
        this.text = text;
        this.width = Minecraft.getInstance().font.width(text);
    }

    public void setCentered(boolean centered) {
        this.centered = centered;
    }
}
