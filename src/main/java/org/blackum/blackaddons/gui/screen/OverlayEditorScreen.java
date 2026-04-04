package org.blackum.blackaddons.gui.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.widget.Button;
import org.blackum.blackaddons.gui.widget.Widget;
import org.blackum.blackaddons.core.config.ConfigManager;

import java.util.ArrayList;
import java.util.List;

public class OverlayEditorScreen extends BaseScreen {

    private boolean isDragging = false;
    private boolean isResizing = false;
    private int dragOffsetX, dragOffsetY;

    private final Screen parent;

    public OverlayEditorScreen(Screen parent) {
        super(Component.literal("Overlay Editor"));
        this.parent = parent;
    }

    @Override
    public void onClose() {
        ConfigManager.save();
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fillGradient(0, 0, this.width, this.height, 0xAA000000, 0xAA000000);

        renderOverlayPreview(graphics);

        float scale = BaseScreen.overlayScale;
        int x = BaseScreen.overlayX;
        int y = BaseScreen.overlayY;

        int boxWidth = (int) (150 * scale);
        int boxHeight = (int) (50 * scale);

        //? if < 1.21.11 {
        /*/^
        graphics.fill(x - 2, y - 2, x + boxWidth + 2, y - 1, 0xFF00FF00); // top
        graphics.fill(x - 2, y + boxHeight + 1, x + boxWidth + 2, y + boxHeight + 2, 0xFF00FF00); // bottom
        graphics.fill(x - 2, y - 1, x - 1, y + boxHeight + 1, 0xFF00FF00); // left
        graphics.fill(x + boxWidth + 1, y - 1, x + boxWidth + 2, y + boxHeight + 1, 0xFF00FF00); // right
        ^/
        graphics.fill(x - 2, y - 2, x + boxWidth + 2, y + boxHeight + 2, 0x0100FF00);
        graphics.fill(x - 2, y - 2, x + boxWidth + 2, y - 1, 0xFF00FF00);
        graphics.fill(x - 2, y + boxHeight + 1, x + boxWidth + 2, y + boxHeight + 2, 0xFF00FF00);
        graphics.fill(x - 2, y - 1, x - 1, y + boxHeight + 1, 0xFF00FF00);
        graphics.fill(x + boxWidth + 1, y - 1, x + boxWidth + 2, y + boxHeight + 1, 0xFF00FF00);
        *///?} else
        graphics.outline(x - 2, y - 2, boxWidth + 4, boxHeight + 4, 0xFF00FF00);

        int handleSize = 8;
        graphics.fill(x + boxWidth - handleSize + 2, y + boxHeight - handleSize + 2, x + boxWidth + 2,
                y + boxHeight + 2, 0xFFFFFFFF);

        for (Widget widget : widgets) {
            if (widget.isVisible()) {
                widget.updateHoverState(mouseX, mouseY);
                widget.extractRenderState(graphics, mouseX, mouseY, partialTick);
            }
        }

        graphics.centeredText(this.font, "Drag to Move | Drag Handle to Resize", this.width / 2, 10, 0xFFFFFFFF);
    }

    private void renderOverlayPreview(GuiGraphicsExtractor graphics) {
        graphics.pose().pushMatrix();
        graphics.pose().translate((float) BaseScreen.overlayX, (float) BaseScreen.overlayY);
        graphics.pose().scale(BaseScreen.overlayScale, BaseScreen.overlayScale);

        int x = 0;
        int y = 0;

        List<String> debugInfo = new ArrayList<>();
        debugInfo.add(ChatFormatting.GOLD + "[BlackAddons Debug]");
        debugInfo.add("VSync: " + this.minecraft.options.enableVsync().get());
        debugInfo.add("Mouse: " + (int) this.minecraft.mouseHandler.xpos() + ", "
                + (int) this.minecraft.mouseHandler.ypos());
        debugInfo.add("Screen: OverlayEditorScreen");

        for (String line : debugInfo) {
            graphics.text(this.font, line, x, y, 0xFFFFFFFF);
            y += 10;
        }

        graphics.pose().popMatrix();
    }

    @Override
    public void init() {
        super.init();
        this.containerX = 0;
        this.containerY = 0;
        this.containerWidth = this.width;
        this.containerHeight = this.height;
        this.widgets.clear();
        initWidgets();
    }

    @Override
    public void initWidgets() {
        int buttonWidth = 100;
        int buttonX = (this.width - buttonWidth) / 2;
        int buttonY = this.height - Theme.BUTTON_HEIGHT - Theme.MARGIN;

        addWidget(new Button(buttonX, buttonY, buttonWidth, "Done", () -> {
            this.onClose();
        }));
    }

    private static final float MIN_SCALE = 0.1f;
    private static final float MAX_SCALE = 10.0f;

    private void setOverlayScale(double newScale) {
        if (newScale < MIN_SCALE)
            newScale = MIN_SCALE;
        if (newScale > MAX_SCALE)
            newScale = MAX_SCALE;
        BaseScreen.overlayScale = (float) newScale;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        float scale = BaseScreen.overlayScale;
        int x = BaseScreen.overlayX;
        int y = BaseScreen.overlayY;
        int boxWidth = (int) (150 * scale);
        int boxHeight = (int) (50 * scale);

        if (mouseX >= x && mouseX <= x + boxWidth && mouseY >= y && mouseY <= y + boxHeight) {
            setOverlayScale(scale + (scrollY * 0.1));
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            Minecraft mc = Minecraft.getInstance();

            double mouseX = getScaledMouseX();
            double mouseY = getScaledMouseY();

            float scale = BaseScreen.overlayScale;
            int x = BaseScreen.overlayX;
            int y = BaseScreen.overlayY;
            int boxWidth = (int) (150 * scale);
            int boxHeight = (int) (50 * scale);

            if (mouseX >= x + boxWidth - 10 && mouseX <= x + boxWidth + 5 &&
                    mouseY >= y + boxHeight - 10 && mouseY <= y + boxHeight + 5) {
                isResizing = true;
                return true;
            }

            if (mouseX >= x && mouseX <= x + boxWidth && mouseY >= y && mouseY <= y + boxHeight) {
                isDragging = true;
                dragOffsetX = (int) (mouseX - x);
                dragOffsetY = (int) (mouseY - y);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        isDragging = false;
        isResizing = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (isDragging || isResizing) {
            double mouseX = getScaledMouseX();
            double mouseY = getScaledMouseY();

            if (isDragging) {
                BaseScreen.overlayX = (int) (mouseX - dragOffsetX);
                BaseScreen.overlayY = (int) (mouseY - dragOffsetY);
                return true;
            }

            if (isResizing) {
                double dx = mouseX - BaseScreen.overlayX;
                double newScale = dx / 150.0;
                setOverlayScale(newScale);
                return true;
            }
        }
        return super.mouseDragged(event, dragX, dragY);
    }
}
