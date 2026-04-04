package org.blackum.blackaddons.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import org.blackum.blackaddons.gui.render.Theme;

public class Label extends Widget {

    public enum Style {
        TITLE(1.2f, Theme.TEXT_PRIMARY),
        SUBTITLE(1.0f, Theme.TEXT_PRIMARY),
        BODY(1.0f, Theme.TEXT_SECONDARY),
        CAPTION(0.9f, Theme.TEXT_SECONDARY);

        public final float scale;
        public final int color;

        Style(float scale, int color) {
            this.scale = scale;
            this.color = color;
        }
    }

    public enum Alignment {
        LEFT, CENTER, RIGHT
    }

    private String text;
    private Style style;
    private Alignment alignment;
    private int customColor = -1;

    public Label(int x, int y, String text) {
        this(x, y, text, Style.BODY, Alignment.LEFT);
    }

    public Label(int x, int y, String text, Style style) {
        this(x, y, text, style, Alignment.LEFT);
    }

    public Label(int x, int y, String text, Style style, Alignment alignment) {
        super(x, y, 0, 0);
        this.text = text;
        this.style = style;
        this.alignment = alignment;
        updateSize();
    }

    private void updateSize() {
        int textWidth = (int) (Minecraft.getInstance().font.width(text) * style.scale);
        int textHeight = (int) (8 * style.scale);
        this.width = textWidth;
        this.height = textHeight;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible)
            return;

        int color = customColor != -1 ? customColor : style.color;
        int textWidth = Minecraft.getInstance().font.width(text);

        int renderX = x;
        if (alignment == Alignment.CENTER) {
            renderX = x + (width - textWidth) / 2;
        } else if (alignment == Alignment.RIGHT) {
            renderX = x + width - textWidth;
        }

        graphics.text(Minecraft.getInstance().font, text, renderX, y, color);
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
        updateSize();
    }

    public Style getStyle() {
        return style;
    }

    public void setStyle(Style style) {
        this.style = style;
        updateSize();
    }

    public Alignment getAlignment() {
        return alignment;
    }

    public void setAlignment(Alignment alignment) {
        this.alignment = alignment;
    }

    public void setColor(int color) {
        this.customColor = color;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, MouseButtonEvent event) {
        return false;
    }
}
