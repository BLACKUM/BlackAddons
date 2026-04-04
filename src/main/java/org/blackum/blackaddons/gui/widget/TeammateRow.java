package org.blackum.blackaddons.gui.widget;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import org.blackum.blackaddons.gui.animation.Animation;
import org.blackum.blackaddons.gui.animation.Easing;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.core.model.Teammate;
import org.blackum.blackaddons.core.util.Constants;
import org.blackum.blackaddons.core.util.FormatUtils;

public class TeammateRow extends Widget {
    private final Teammate tm;
    private Animation hoverAnimation;
    private final Button inviteBtn;

    public static final int COL_IGN = 90;
    public static final int COL_RUNS = 40;
    public static final int COL_CLASS = 80;
    public static final int COL_FLOOR = 45;

    public TeammateRow(int width, Teammate tm) {
        super(0, 0, width, 18);
        this.tm = tm;
        this.hoverAnimation = new Animation(0, 1, Theme.ANIM_HOVER, Easing::easeOut);
        this.inviteBtn = new Button(0, 0, 40, 12, "Invite", () -> {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.connection.sendCommand("party " + tm.ign);
            }
        });
    }

    @Override
    public void updateHoverState(int mouseX, int mouseY) {
        super.updateHoverState(mouseX, mouseY);
        inviteBtn.setX(this.x + this.width - 45);
        inviteBtn.setY(this.y + 3);
        inviteBtn.updateHoverState(mouseX, mouseY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible)
            return;

        float hover = hoverAnimation.getValue();
        if (hover > 0) {
            int color = Theme.withAlpha(Theme.GLASS_HIGHLIGHT, hover * 0.2f);
            graphics.fill(x, y, x + width, y + height, color);
        }

        int cx = x + 2;
        int cy = y + 5;

        graphics.text(Minecraft.getInstance().font, tm.ign, cx, cy, Theme.ACCENT);
        cx += COL_IGN;
        graphics.text(Minecraft.getInstance().font, ChatFormatting.WHITE + String.valueOf(tm.count), cx, cy,
                0xFFFFFFFF);
        cx += COL_RUNS;

        String classText = String.format("%s%s %d", ChatFormatting.WHITE, tm.lastClass, tm.lastClassLevel);
        graphics.text(Minecraft.getInstance().font, classText, cx, cy, 0xFFFFFFFF);
        cx += COL_CLASS;

        graphics.text(Minecraft.getInstance().font, ChatFormatting.WHITE + tm.lastFloor, cx, cy, 0xFFFFFFFF);
        cx += COL_FLOOR;

        String timeAgo = FormatUtils.formatRelativeTime(tm.lastTs);
        graphics.text(Minecraft.getInstance().font, ChatFormatting.GRAY + timeAgo, cx, cy, 0xFFFFFFFF);

        inviteBtn.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void tick() {
        inviteBtn.tick();
        if (hovered && hoverAnimation.getProgress() < 1
                && (!hoverAnimation.isRunning() || hoverAnimation.getValue() < 1)) {
            hoverAnimation = new Animation(hoverAnimation.getValue(), 1, Theme.ANIM_HOVER, Easing::easeOut);
            hoverAnimation.start();
        } else if (!hovered && hoverAnimation.getProgress() > 0
                && (!hoverAnimation.isRunning() || hoverAnimation.getValue() > 0)) {
            hoverAnimation = new Animation(hoverAnimation.getValue(), 0, Theme.ANIM_HOVER, Easing::easeOut);
            hoverAnimation.start();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, MouseButtonEvent event) {
        if (!visible || !enabled)
            return false;
        if (inviteBtn.mouseClicked(mouseX, mouseY, event))
            return true;

        if (isMouseOver(mouseX, mouseY) && event.buttonInfo().button() == 0) {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.connection.sendCommand(Constants.BASE_COMMAND + " pv " + tm.ign);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, MouseButtonEvent event) {
        return inviteBtn.mouseReleased(mouseX, mouseY, event);
    }
}
