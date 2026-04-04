package org.blackum.blackaddons.gui.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.blackum.blackaddons.gui.render.Theme;
import net.minecraft.ChatFormatting;
import org.blackum.blackaddons.gui.notification.NotificationManager;
import org.blackum.blackaddons.gui.notification.NotificationType;
import org.blackum.blackaddons.feature.chat.ImageHelper;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.input.MouseButtonEvent;

public class ImagePreviewScreen extends BaseScreen {
    private final String imageUrl;
    private ImageHelper.ImageInfo imageInfo;
    private boolean loading = true;
    private float progress = 0.0f;
    private String error = null;

    public ImagePreviewScreen(String imageUrl) {
        this(imageUrl, null);
    }

    public ImagePreviewScreen(String imageUrl, net.minecraft.client.gui.screens.Screen parent) {
        super(Component.literal("Image Preview"), parent);
        this.imageUrl = imageUrl;
    }

    @Override
    public void initWidgets() {
        loading = true;
        progress = 0.0f;
        error = null;
        ImageHelper.downloadImage(imageUrl, p -> this.progress = p).thenAccept(info -> {
            if (info != null) {
                this.imageInfo = info;
                this.loading = false;
            } else {
                this.error = "Failed to load image (result null)";
                this.loading = false;
                NotificationManager.addNotification("Image Preview", ChatFormatting.RED + "Failed to load image.",
                        NotificationType.ERROR);
            }
        }).exceptionally(ex -> {
            this.error = "Error: " + ex.getMessage();
            this.loading = false;
            NotificationManager.addNotification("Image Preview", ChatFormatting.RED + "Error: " + ex.getMessage(),
                    NotificationType.ERROR);
            return null;
        });
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        graphics.fill(0, 0, this.width, this.height, 0xAA000000);

        if (loading) {
            String text = "Downloading... " + (int) (progress * 100) + "%";
            int textX = (this.width - this.font.width(text)) / 2;
            int textY = this.height / 2 - 10;
            graphics.text(this.font, text, textX, textY, 0xFFFFFF);

            int barW = 200;
            int barH = 4;
            int barX = (this.width - barW) / 2;
            int barY = this.height / 2 + 5;

            graphics.fill(barX, barY, barX + barW, barY + barH, 0x44FFFFFF);
            int progressW = (int) (barW * progress);
            graphics.fill(barX, barY, barX + progressW, barY + barH, Theme.ACCENT);
        } else if (error != null) {
            graphics.text(this.font, error, (this.width - this.font.width(error)) / 2, this.height / 2, 0xFF5555);
        } else if (imageInfo != null && !imageInfo.frames().isEmpty()) {
            ImageHelper.FrameInfo currentFrame = getCurrentFrame();

            int imgW = imageInfo.width();
            int imgH = imageInfo.height();

            float maxW = this.width * 0.9f;
            float maxH = this.height * 0.9f;
            float scale = Math.min(maxW / imgW, maxH / imgH);
            if (scale > 1.0f)
                scale = 1.0f;

            int drawW = (int) (imgW * scale);
            int drawH = (int) (imgH * scale);
            int x = (this.width - drawW) / 2;
            int y = (this.height - drawH) / 2;

            graphics.blit(RenderPipelines.GUI_TEXTURED, currentFrame.location(), x, y, 0f, 0f, drawW, drawH, imgW, imgH,
                    imgW, imgH);
        }
    }

    private ImageHelper.FrameInfo getCurrentFrame() {
        if (imageInfo.frames().size() == 1) {
            return imageInfo.frames().get(0);
        }

        int totalDuration = 0;
        for (ImageHelper.FrameInfo frame : imageInfo.frames()) {
            totalDuration += frame.delay();
        }

        if (totalDuration <= 0)
            return imageInfo.frames().get(0);

        long currentTime = System.currentTimeMillis() % totalDuration;
        int elapsed = 0;
        for (ImageHelper.FrameInfo frame : imageInfo.frames()) {
            elapsed += frame.delay();
            if (currentTime < elapsed) {
                return frame;
            }
        }

        return imageInfo.frames().get(0);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        this.onClose();
        return true;
    }
}
