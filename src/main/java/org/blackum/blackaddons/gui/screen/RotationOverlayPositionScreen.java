package org.blackum.blackaddons.gui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.input.MouseButtonEvent;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.gui.render.Theme;

public class RotationOverlayPositionScreen extends Screen {
    private static final int PREVIEW_WIDTH = 145;
    private static final int PREVIEW_HEIGHT = 55;
    private static final int BORDER_COLOR = 0xFF3B82F6;
    private static final int BG_COLOR = 0xC0111827;
    private static final int HINT_COLOR = 0xFFAAAAAA;

    private final Screen parent;
    private boolean dragging;
    private double dragOffsetX;
    private double dragOffsetY;
    private int overlayX;
    private int overlayY;

    public RotationOverlayPositionScreen(Screen parent) {
        super(Component.literal("Rotation Overlay Position"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        overlayX = ConfigManager.data.rotationOverlayX < 0
                ? this.width - PREVIEW_WIDTH - 5
                : ConfigManager.data.rotationOverlayX;
        overlayY = ConfigManager.data.rotationOverlayY;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, 0x80000000);
        g.drawCenteredString(font, "Drag the overlay box to reposition it. Press Esc to save.", this.width / 2, this.height / 2, HINT_COLOR);
        g.drawCenteredString(font, "Right-click to reset to default (top-right).", this.width / 2, this.height / 2 + 12, HINT_COLOR);

        g.fill(overlayX, overlayY, overlayX + PREVIEW_WIDTH, overlayY + PREVIEW_HEIGHT, BG_COLOR);
        g.fill(overlayX, overlayY, overlayX + PREVIEW_WIDTH, overlayY + 1, BORDER_COLOR);
        g.fill(overlayX, overlayY + PREVIEW_HEIGHT - 1, overlayX + PREVIEW_WIDTH, overlayY + PREVIEW_HEIGHT, BORDER_COLOR);
        g.fill(overlayX, overlayY, overlayX + 1, overlayY + PREVIEW_HEIGHT, BORDER_COLOR);
        g.fill(overlayX + PREVIEW_WIDTH - 1, overlayY, overlayX + PREVIEW_WIDTH, overlayY + PREVIEW_HEIGHT, BORDER_COLOR);

        int ty = overlayY + 5;
        g.drawString(font, "§6[Rotation] §aACTIVE", overlayX + 5, ty, 0xFFFFFFFF);
        ty += 10;
        g.drawString(font, "Yaw:   90.0 -> 45.0", overlayX + 5, ty, 0xFFFFFFFF);
        ty += 10;
        g.drawString(font, "Pitch: 0.0 -> -30.0", overlayX + 5, ty, 0xFFFFFFFF);
        ty += 10;
        g.fill(overlayX + 5, ty, overlayX + 5 + 100, ty + 4, 0xFF333333);
        g.fill(overlayX + 5, ty, overlayX + 5 + 60, ty + 4, 0xFF00AAFF);
        g.drawString(font, " 60%", overlayX + 110, ty - 2, 0xFF999999);
    }

    private double getScaledMouseX() {
        Minecraft mc = Minecraft.getInstance();
        return mc.mouseHandler.xpos() * ((double) this.width / mc.getWindow().getScreenWidth());
    }

    private double getScaledMouseY() {
        Minecraft mc = Minecraft.getInstance();
        return mc.mouseHandler.ypos() * ((double) this.height / mc.getWindow().getScreenHeight());
    }

    private boolean isOverPreview(double mx, double my) {
        return mx >= overlayX && mx <= overlayX + PREVIEW_WIDTH
                && my >= overlayY && my <= overlayY + PREVIEW_HEIGHT;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean pressed) {
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
            overlayY = 5;
            return true;
        }
        return super.mouseClicked(event, pressed);
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
        ConfigManager.data.rotationOverlayX = overlayX;
        ConfigManager.data.rotationOverlayY = overlayY;
        ConfigManager.save();
        Minecraft.getInstance().setScreen(parent);
    }
}
