package org.blackum.blackaddons.gui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.input.MouseButtonEvent;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.gui.render.RenderHelper;

public class AutoSSOverlayPositionScreen extends BaseScreen {
    @Override
    public void initWidgets() {}

    private static final int PREVIEW_WIDTH = 145;
    private static final int PREVIEW_HEIGHT = 80;
    private static final int BORDER_COLOR = 0xFFFACC15;
    private static final int BG_COLOR = 0xC0111827;
    private static final int HINT_COLOR = 0xFFAAAAAA;

    private final Screen parent;
    private boolean dragging;
    private double dragOffsetX;
    private double dragOffsetY;
    private int overlayX;
    private int overlayY;

    public AutoSSOverlayPositionScreen(Screen parent) {
        super(Component.literal("AutoSS Overlay Position"));
        this.parent = parent;
    }

    @Override
    public void init() {
        overlayX = ConfigManager.data.AutoSSOverlayX < 0
                ? 10
                : ConfigManager.data.AutoSSOverlayX;
        overlayY = ConfigManager.data.AutoSSOverlayY;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, 0x80000000);
        RenderHelper.drawCenteredString(g, font, "Drag the AutoSS debug box to reposition it.", this.width / 2, this.height / 2, HINT_COLOR);
        RenderHelper.drawCenteredString(g, font, "Press Esc to save.", this.width / 2, this.height / 2 + 12, HINT_COLOR);

        g.fill(overlayX, overlayY, overlayX + PREVIEW_WIDTH, overlayY + PREVIEW_HEIGHT, BG_COLOR);
        g.fill(overlayX, overlayY, overlayX + PREVIEW_WIDTH, overlayY + 1, BORDER_COLOR);
        g.fill(overlayX, overlayY + PREVIEW_HEIGHT - 1, overlayX + PREVIEW_WIDTH, overlayY + PREVIEW_HEIGHT, BORDER_COLOR);
        g.fill(overlayX, overlayY, overlayX + 1, overlayY + PREVIEW_HEIGHT, BORDER_COLOR);
        g.fill(overlayX + PREVIEW_WIDTH - 1, overlayY, overlayX + PREVIEW_WIDTH, overlayY + PREVIEW_HEIGHT, BORDER_COLOR);

        int ty = overlayY + 5;
        g.text(font, "§6[AutoSS Debug] §aSolving...", overlayX + 5, ty, 0xFFFFFFFF);
        ty += 10;
        g.text(font, "Target: 110, 121, 91", overlayX + 5, ty, 0xFFFFFFFF);
        ty += 10;
        g.text(font, "On Target: §aYES", overlayX + 5, ty, 0xFFFFFFFF);
        ty += 10;
        g.text(font, "Rot Done: §aYES", overlayX + 5, ty, 0xFFFFFFFF);
        ty += 10;
        g.text(font, "Rot: 90.0, -30.0", overlayX + 5, ty, 0xFFFFFFFF);
    }

    private boolean isOverPreview(double mx, double my) {
        return mx >= overlayX && mx <= overlayX + PREVIEW_WIDTH
                && my >= overlayY && my <= overlayY + PREVIEW_HEIGHT;
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
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = false;
        return super.mouseReleased(event);
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
    public void onClose() {
        ConfigManager.data.AutoSSOverlayX = overlayX;
        ConfigManager.data.AutoSSOverlayY = overlayY;
        ConfigManager.save();
        Minecraft.getInstance().setScreen(parent);
    }
}
