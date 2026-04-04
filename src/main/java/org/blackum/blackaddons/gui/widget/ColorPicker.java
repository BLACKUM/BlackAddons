package org.blackum.blackaddons.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.render.ColorUtils;
import org.blackum.blackaddons.gui.render.RenderHelper;

import java.util.function.Consumer;

public class ColorPicker extends Widget {

    public static final int WIDTH = 180;
    public static final int HEIGHT = 210;

    private float hull = 0f;
    private float saturation = 1f;
    private float brightness = 1f;
    private float alpha = 1f;
    private Consumer<Integer> onColorChange;

    private boolean draggingHue = false;
    private boolean draggingSB = false;
    private boolean draggingAlpha = false;

    private int gridX;
    private TextField rgbaField;
    private boolean isUpdatingFields = false;

    public ColorPicker(int x, int y, Consumer<Integer> onColorChange) {
        this(x, y, 0xFF00A8FF, onColorChange);
    }

    public ColorPicker(int x, int y, int initialColor, Consumer<Integer> onColorChange) {
        super(x, y, WIDTH, HEIGHT);
        this.onColorChange = onColorChange;

        float[] hsb = ColorUtils.toHSB(initialColor);
        this.hull = hsb[0];
        this.saturation = hsb[1];
        this.brightness = hsb[2];
        this.alpha = ((initialColor >> 24) & 0xFF) / 255f;

        this.rgbaField = new TextField(0, 0, 100, 16, "RGBA");
        this.rgbaField.setMaxLength(16);
        this.rgbaField.setCharFilter(c -> Character.isDigit(c) || c == ',' || c == ' ');

        updateFields();
    }

    @Override
    public void setX(int x) {
        this.gridX = x;
        updateCenteredX();
    }

    @Override
    public void setWidth(int width) {
        updateCenteredX();
    }

    private void updateCenteredX() {
        this.x = gridX;
        this.width = WIDTH;
    }

    private void updateFields() {
        if (isUpdatingFields)
            return;
        isUpdatingFields = true;
        int color = ColorUtils.hsvToRgb(hull, saturation, brightness);
        int packedColor = ((int) (alpha * 255) << 24) | (color & 0xFFFFFF);

        rgbaField.setText(ColorUtils.toRGBA(packedColor));
        isUpdatingFields = false;
    }

    private void onFieldUpdate() {
        if (isUpdatingFields)
            return;

        Integer color = ColorUtils.parseRGBA(rgbaField.getText());
        if (color != null) {
            isUpdatingFields = true;
            float[] hsb = ColorUtils.toHSB(color);
            this.hull = hsb[0];
            this.saturation = hsb[1];
            this.brightness = hsb[2];
            this.alpha = ((color >> 24) & 0xFF) / 255f;
            notifyChange();
            isUpdatingFields = false;
        }
    }

    @Override
    public void tick() {
        rgbaField.tick();

        if (rgbaField.isFocused()) {
            onFieldUpdate();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible)
            return;

        RenderHelper.renderSurface(graphics, x, y, width, height, Theme.BORDER_RADIUS, false);

        int currentY = y + 10;
        int sbWidth = 160;
        int sbHeight = 120;
        int innerX = x + (width - sbWidth) / 2;

        renderSBArea(graphics, innerX, currentY, sbWidth, sbHeight);
        currentY += sbHeight + 10;

        renderHueSlider(graphics, innerX, currentY, sbWidth, 8);
        currentY += 16;

        renderAlphaSlider(graphics, innerX, currentY, sbWidth, 8);
        currentY += 16;

        renderValues(graphics, mouseX, mouseY, partialTick, innerX, currentY, sbWidth);
    }

    private void renderSBArea(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        int baseColor = ColorUtils.hsvToRgb(hull, 1f, 1f);
        graphics.fill(x, y, x + width, y + height, baseColor | 0xFF000000);

        for (int i = 0; i <= width; i += 2) {
            float a = 1.0f - (float) i / width;
            int color = ((int) (a * 255) << 24) | 0xFFFFFF;
            graphics.fill(x + i, y, x + i + 2, y + height, color);
        }

        for (int i = 0; i <= height; i += 2) {
            float a = (float) i / height;
            int color = ((int) (a * 255) << 24) | 0x000000;
            graphics.fill(x, y + i, x + width, y + i + 2, color);
        }

        int cursorX = x + (int) (saturation * width);
        int cursorY = y + (int) ((1f - brightness) * height);

        //? if < 1.21.11 {
        /*graphics.fill(cursorX - 4, cursorY - 4, cursorX + 4, cursorY - 3, Theme.TEXT_PRIMARY);
        graphics.fill(cursorX - 4, cursorY + 3, cursorX + 4, cursorY + 4, Theme.TEXT_PRIMARY);
        graphics.fill(cursorX - 4, cursorY - 3, cursorX - 3, cursorY + 3, Theme.TEXT_PRIMARY);
        graphics.fill(cursorX + 3, cursorY - 3, cursorX + 4, cursorY + 3, Theme.TEXT_PRIMARY);
        *///?} else
        graphics.outline(cursorX - 4, cursorY - 4, 8, 8, Theme.TEXT_PRIMARY);
    }

    private void renderHueSlider(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        for (int i = 0; i < width; i++) {
            float h = (float) i / width;
            int color = ColorUtils.hsvToRgb(h, 1f, 1f);
            graphics.fill(x + i, y, x + i + 1, y + height, color | 0xFF000000);
        }

        int selectorX = x + (int) (hull * width);
        graphics.fill(selectorX - 2, y - 2, selectorX + 2, y + height + 2, Theme.TEXT_PRIMARY);
    }

    private void renderAlphaSlider(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        int baseColor = ColorUtils.hsvToRgb(hull, saturation, brightness) & 0xFFFFFF;

        graphics.fill(x, y, x + width, y + height, Theme.SURFACE_LIGHT);

        for (int i = 0; i < width; i++) {
            float a = (float) i / width;
            int color = ((int) (a * 255) << 24) | baseColor;
            graphics.fill(x + i, y, x + i + 1, y + height, color);
        }

        int selectorX = x + (int) (alpha * width);
        graphics.fill(selectorX - 2, y - 2, selectorX + 2, y + height + 2, Theme.TEXT_PRIMARY);
    }

    private void renderValues(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, int x, int y,
            int width) {
        int currentColor = ColorUtils.hsvToRgb(hull, saturation, brightness);
        int packedColor = ((int) (alpha * 255) << 24) | (currentColor & 0xFFFFFF);

        int previewSize = 24;
        RenderHelper.renderSurface(graphics, x, y, previewSize, previewSize, Theme.BORDER_RADIUS_SMALL, false);
        graphics.fill(x + 1, y + 1, x + previewSize - 1, y + previewSize - 1, packedColor);

        int fieldX = x + previewSize + 6;
        int fieldWidth = width - previewSize - 6;

        rgbaField.setX(fieldX);
        rgbaField.setY(y + 8);
        rgbaField.setWidth(fieldWidth);
        rgbaField.extractRenderState(graphics, mouseX, mouseY, partialTick);

        graphics.text(Minecraft.getInstance().font, "RGBA:", fieldX + 3, y, Theme.TEXT_SECONDARY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, MouseButtonEvent event) {
        int sbWidth = 160;
        int sbHeight = 120;
        int innerX = x + (width - sbWidth) / 2;
        int sbY = y + 10;

        if (mouseX >= innerX && mouseX <= innerX + sbWidth && mouseY >= sbY && mouseY <= sbY + sbHeight) {
            draggingSB = true;
            updateSB(mouseX, mouseY);
            rgbaField.setFocused(false);
            return true;
        }

        int hueY = sbY + sbHeight + 10;
        if (mouseX >= innerX && mouseX <= innerX + sbWidth && mouseY >= hueY && mouseY <= hueY + 8) {
            draggingHue = true;
            updateHue(mouseX);
            rgbaField.setFocused(false);
            return true;
        }

        int alphaY = hueY + 16;
        if (mouseX >= innerX && mouseX <= innerX + sbWidth && mouseY >= alphaY && mouseY <= alphaY + 8) {
            draggingAlpha = true;
            updateAlpha(mouseX);
            rgbaField.setFocused(false);
            return true;
        }

        if (rgbaField.isMouseOver(mouseX, mouseY)) {
            rgbaField.mouseClicked(mouseX, mouseY, event);
            rgbaField.setFocused(true);
            return true;
        }

        rgbaField.setFocused(false);
        return super.mouseClicked(mouseX, mouseY, event);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, MouseButtonEvent event) {
        draggingHue = false;
        draggingSB = false;
        draggingAlpha = false;
        if (rgbaField.isFocused()) {
            return rgbaField.mouseReleased(mouseX, mouseY, event);
        }
        return super.mouseReleased(mouseX, mouseY, event);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, MouseButtonEvent event, double dragX, double dragY) {
        if (draggingHue) {
            updateHue(mouseX);
            return true;
        }
        if (draggingSB) {
            updateSB(mouseX, mouseY);
            return true;
        }
        if (draggingAlpha) {
            updateAlpha(mouseX);
            return true;
        }
        if (rgbaField.isFocused()) {
            return rgbaField.mouseDragged(mouseX, mouseY, event, dragX, dragY);
        }
        return super.mouseDragged(mouseX, mouseY, event, dragX, dragY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, KeyEvent event) {
        if (rgbaField.isFocused())
            return rgbaField.keyPressed(keyCode, scanCode, event);
        return super.keyPressed(keyCode, scanCode, event);
    }

    @Override
    public boolean charTyped(char chr, CharacterEvent event) {
        if (rgbaField.isFocused())
            return rgbaField.charTyped(chr, event);
        return super.charTyped(chr, event);
    }

    private void updateHue(double mouseX) {
        int sbWidth = 160;
        int innerX = x + (width - sbWidth) / 2;
        hull = (float) ((mouseX - innerX) / sbWidth);
        hull = Math.max(0f, Math.min(1f, hull));
        updateFields();
        notifyChange();
    }

    private void updateSB(double mouseX, double mouseY) {
        int sbWidth = 160;
        int sbHeight = 120;
        int innerX = x + (width - sbWidth) / 2;
        int sbY = y + 10;
        saturation = (float) ((mouseX - innerX) / sbWidth);
        brightness = 1f - (float) ((mouseY - sbY) / sbHeight);
        saturation = Math.max(0f, Math.min(1f, saturation));
        brightness = Math.max(0f, Math.min(1f, brightness));
        updateFields();
        notifyChange();
    }

    private void updateAlpha(double mouseX) {
        int sbWidth = 160;
        int innerX = x + (width - sbWidth) / 2;
        alpha = (float) ((mouseX - innerX) / sbWidth);
        alpha = Math.max(0f, Math.min(1f, alpha));
        updateFields();
        notifyChange();
    }

    private void notifyChange() {
        if (onColorChange != null) {
            int color = ColorUtils.hsvToRgb(hull, saturation, brightness);
            int packedColor = ((int) (alpha * 255) << 24) | (color & 0xFFFFFF);
            onColorChange.accept(packedColor);
        }
    }
}
