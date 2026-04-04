package org.blackum.blackaddons.gui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.gui.render.RenderHelper;

public class AlignOverlayPositionScreen extends BaseScreen {
    @Override
    public void initWidgets() {}

    private static final int PREVIEW_WIDTH = 190;
    private static final int PREVIEW_HEIGHT = 90;
    private static final int BORDER_COLOR = 0xFFF59E0B;
    private static final int BG_COLOR = 0xC0111827;
    private static final int HINT_COLOR = 0xFFAAAAAA;

    private final Screen parent;
    private boolean dragging;
    private double dragOffsetX;
    private double dragOffsetY;
    private int overlayX;
    private int overlayY;

    public AlignOverlayPositionScreen(Screen parent) {
        super(Component.literal("Align Overlay Position"));
        this.parent = parent;
    }

    @Override
    public void init() {
        overlayX = ConfigManager.data.alignOverlayX < 0
                ? this.width - PREVIEW_WIDTH - 5
                : ConfigManager.data.alignOverlayX;
        overlayY = ConfigManager.data.alignOverlayY;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, 0x80000000);
        RenderHelper.drawCenteredString(g, font, "Drag the align debug box to reposition it.", this.width / 2, this.height / 2, HINT_COLOR);
        RenderHelper.drawCenteredString(g, font, "Right-click to reset. Press Esc to save.", this.width / 2, this.height / 2 + 12, HINT_COLOR);

        g.fill(overlayX, overlayY, overlayX + PREVIEW_WIDTH, overlayY + PREVIEW_HEIGHT, BG_COLOR);
        g.fill(overlayX, overlayY, overlayX + PREVIEW_WIDTH, overlayY + 1, BORDER_COLOR);
        g.fill(overlayX, overlayY + PREVIEW_HEIGHT - 1, overlayX + PREVIEW_WIDTH, overlayY + PREVIEW_HEIGHT, BORDER_COLOR);
        g.fill(overlayX, overlayY, overlayX + 1, overlayY + PREVIEW_HEIGHT, BORDER_COLOR);
        g.fill(overlayX + PREVIEW_WIDTH - 1, overlayY, overlayX + PREVIEW_WIDTH, overlayY + PREVIEW_HEIGHT, BORDER_COLOR);

        int ty = overlayY + 5;
        g.text(font, "§6[Align Debug]", overlayX + 5, ty, 0xFFFFFFFF);
        ty += 10;
        g.text(font, "State: ACTIVE/1", overlayX + 5, ty, 0xFFFFFFFF);
        ty += 10;
        g.text(font, "Target: 12.5000 34.5000", overlayX + 5, ty, 0xFFFFFFFF);
        ty += 10;
        g.text(font, "Expected: 12.4999 34.5001", overlayX + 5, ty, 0xFFFFFFFF);
        ty += 10;
        g.text(font, "Expected err: 0.000141", overlayX + 5, ty, 0xFFFFFFFF);
        ty += 10;
        g.text(font, "Actual@+0.5s: 12.4500 34.4970", overlayX + 5, ty, 0xFFFFFFFF);
        ty += 10;
        g.text(font, "Actual err: 0.050090", overlayX + 5, ty, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = getScaledMouseX();
        double my = getScaledMouseY();
        int button = event.button();
        if (button == 0 && isOverPreview(mx, my)) {
            dragging = true;
            dragOffsetX = mx - overlayX;
            dragOffsetY = my - overlayY;
            return true;
        }
        if (button == 1) {
            overlayX = this.width - PREVIEW_WIDTH - 5;
            overlayY = 125;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (dragging) {
            double mx = getScaledMouseX();
            double my = getScaledMouseY();
            overlayX = (int) Math.max(0, Math.min(this.width - PREVIEW_WIDTH, mx - dragOffsetX));
            overlayY = (int) Math.max(0, Math.min(this.height - PREVIEW_HEIGHT, my - dragOffsetY));
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = false;
        return super.mouseReleased(event);
    }

    @Override
    public void onClose() {
        ConfigManager.data.alignOverlayX = overlayX;
        ConfigManager.data.alignOverlayY = overlayY;
        ConfigManager.save();
        Minecraft.getInstance().setScreen(parent);
    }
    
    private boolean isOverPreview(double mx, double my) {
        return mx >= overlayX && mx <= overlayX + PREVIEW_WIDTH
                && my >= overlayY && my <= overlayY + PREVIEW_HEIGHT;
    }
}
