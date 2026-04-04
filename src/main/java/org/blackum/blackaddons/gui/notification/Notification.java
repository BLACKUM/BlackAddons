package org.blackum.blackaddons.gui.notification;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.gui.animation.Animation;
import org.blackum.blackaddons.gui.animation.Easing;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.render.RenderHelper;

import java.util.List;

public class Notification {
    private static final int WIDTH = 160;
    private static final int MARGIN_X = 10;
    private static final int MARGIN_Y = 8;
    private static final int TITLE_HEIGHT = 9;
    private static final int GAP_Y = 4;

    private final String title;
    private final List<FormattedCharSequence> messageLines;
    private final NotificationType type;
    private final int height;

    private final Animation slideAnimation;
    private final Animation exitAnimation;
    private final long creationTime;
    private boolean expiring = false;

    public Notification(String title, String message, NotificationType type) {
        this.title = title;
        this.type = type;
        this.creationTime = System.currentTimeMillis();

        Minecraft mc = Minecraft.getInstance();
        int maxTextWidth = WIDTH - (MARGIN_X * 2);
        this.messageLines = mc.font.split(Component.literal(message), maxTextWidth);

        int linesHeight = messageLines.size() * mc.font.lineHeight;

        this.height = Math.max(40, MARGIN_Y + TITLE_HEIGHT + GAP_Y + linesHeight + MARGIN_Y);

        this.slideAnimation = new Animation(0f, 1f, Theme.ANIM_NORMAL, Easing::easeOut);
        this.exitAnimation = new Animation(1f, 0f, Theme.ANIM_NORMAL, Easing::easeIn);
        this.slideAnimation.start();
    }

    public void tick() {
        if (!expiring && System.currentTimeMillis()
                - creationTime > ConfigManager.data.notificationDuration) {
            expiring = true;
            exitAnimation.start();
        }
    }

    public boolean isExpired() {
        return expiring && exitAnimation.isFinished();
    }

    public void render(GuiGraphicsExtractor graphics, int x, int y) {
        float animValue = slideAnimation.getValue();

        if (expiring) {
            animValue = exitAnimation.getValue();
        }

        int offsetX = (int) ((1f - animValue) * (WIDTH + 20));
        int renderX = x + offsetX;

        RenderHelper.renderRoundedRect(graphics, renderX, y, WIDTH, height,
                Theme.BORDER_RADIUS_SMALL, Theme.GLASS_FILL);

        RenderHelper.renderRoundedOutline(graphics, renderX, y, WIDTH, height,
                Theme.BORDER_RADIUS_SMALL, Theme.GLASS_BORDER);

        RenderHelper.renderRoundedRect(graphics, renderX, y, 3, height,
                Theme.BORDER_RADIUS_SMALL, type.getColor());

        Minecraft mc = Minecraft.getInstance();

        graphics.text(mc.font, title, renderX + MARGIN_X, y + MARGIN_Y, type.getColor(), false);

        int textStart = y + MARGIN_Y + TITLE_HEIGHT + GAP_Y;
        int textY = textStart;
        for (FormattedCharSequence line : messageLines) {
            graphics.text(mc.font, line, renderX + MARGIN_X, textY, Theme.TEXT_SECONDARY, false);
            textY += mc.font.lineHeight;
        }
    }

    public int getHeight() {
        return height;
    }
}
