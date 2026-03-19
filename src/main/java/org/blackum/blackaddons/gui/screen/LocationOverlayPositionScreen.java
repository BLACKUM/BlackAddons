package org.blackum.blackaddons.gui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.blackum.blackaddons.core.config.ConfigManager;

public class LocationOverlayPositionScreen extends Screen {
    private static final int PREVIEW_WIDTH = 165;
    private static final int PREVIEW_HEIGHT = 100;
    private static final int BORDER_COLOR = 0xFF22C55E;
    private static final int BG_COLOR = 0xC0111827;
    private static final int HINT_COLOR = 0xFFAAAAAA;

    private final Screen parent;
    private boolean dragging;
    private double dragOffsetX;
    private double dragOffsetY;
    private int overlayX;
    private int overlayY;

    public LocationOverlayPositionScreen(Screen parent) {
        super(Component.literal("Location Overlay Position"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        overlayX = ConfigManager.data.locationOverlayX < 0
                ? this.width - PREVIEW_WIDTH - 5
                : ConfigManager.data.locationOverlayX;
        overlayY = ConfigManager.data.locationOverlayY;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, 0x80000000);
        g.drawCenteredString(font, "Drag the location debug box to reposition it.", this.width / 2, this.height / 2, HINT_COLOR);
        g.drawCenteredString(font, "Right-click to reset. Press Esc to save.", this.width / 2, this.height / 2 + 12, HINT_COLOR);

        g.fill(overlayX, overlayY, overlayX + PREVIEW_WIDTH, overlayY + PREVIEW_HEIGHT, BG_COLOR);
        g.fill(overlayX, overlayY, overlayX + PREVIEW_WIDTH, overlayY + 1, BORDER_COLOR);
        g.fill(overlayX, overlayY + PREVIEW_HEIGHT - 1, overlayX + PREVIEW_WIDTH, overlayY + PREVIEW_HEIGHT, BORDER_COLOR);
        g.fill(overlayX, overlayY, overlayX + 1, overlayY + PREVIEW_HEIGHT, BORDER_COLOR);
        g.fill(overlayX + PREVIEW_WIDTH - 1, overlayY, overlayX + PREVIEW_WIDTH, overlayY + PREVIEW_HEIGHT, BORDER_COLOR);

        int ty = overlayY + 5;
        g.drawString(font, "§6[Location Utils]", overlayX + 5, ty, 0xFFFFFFFF);
        ty += 10;
        g.drawString(font, "Location: Catacombs (F7)", overlayX + 5, ty, 0xFFFFFFFF);
        ty += 10;
        g.drawString(font, "Dungeons: §aYES", overlayX + 5, ty, 0xFFFFFFFF);
        ty += 10;
        g.drawString(font, "Boss: §cNO", overlayX + 5, ty, 0xFFFFFFFF);
        ty += 10;
        g.drawString(font, "F7 Phase: N/A", overlayX + 5, ty, 0xFFFFFFFF);
        ty += 10;
        g.drawString(font, "Player: 12.0 70.0 44.0", overlayX + 5, ty, 0xFFFFFFFF);
        ty += 10;
        g.drawString(font, "Yaw/Pitch: 90.0 12.5", overlayX + 5, ty, 0xFFFFFFFF);
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
            overlayY = 65;
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
        ConfigManager.data.locationOverlayX = overlayX;
        ConfigManager.data.locationOverlayY = overlayY;
        ConfigManager.save();
        Minecraft.getInstance().setScreen(parent);
    }
}
