package org.blackum.blackaddons.gui.widget;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.render.RenderHelper;

public class FloorCardWidget extends Widget {
    private final String title;
    private final int runs;
    private final int bestScore;
    private final String sPlus;
    private final String s;

    public FloorCardWidget(int width, String title, int runs, int bestScore, String sPlus, String s) {
        super(0, 0, width, 50);
        this.title = title;
        this.runs = runs;
        this.bestScore = bestScore;
        this.sPlus = sPlus;
        this.s = s;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible)
            return;

        RenderHelper.renderRoundedRect(graphics, x, y, width, height, Theme.BORDER_RADIUS, Theme.BACKGROUND_SECONDARY);

        graphics.text(Minecraft.getInstance().font, ChatFormatting.BOLD + title, x + 6, y + 6, Theme.ACCENT);

        String runsText = ChatFormatting.WHITE + String.valueOf(runs) + " Runs";
        int runsWidth = Minecraft.getInstance().font.width(runsText);
        graphics.text(Minecraft.getInstance().font, runsText, x + width - runsWidth - 6, y + 6, 0xFFFFFFFF);

        graphics.fill(x + 6, y + 18, x + width - 6, y + 19, Theme.BACKGROUND_TERTIARY);

        int statY = y + 24;
        int col1X = x + 6;
        int col2X = x + width / 2 + 4;

        graphics.text(Minecraft.getInstance().font,
                ChatFormatting.GRAY + "Best Score: " + ChatFormatting.WHITE + bestScore, col1X, statY, 0xFFFFFFFF);

        graphics.text(Minecraft.getInstance().font, ChatFormatting.GRAY + "S+: " + ChatFormatting.WHITE + sPlus,
                col1X, statY + 12, 0xFFFFFFFF);
        graphics.text(Minecraft.getInstance().font, ChatFormatting.GRAY + "S: " + ChatFormatting.WHITE + s, col2X,
                statY + 12, 0xFFFFFFFF);
    }
}
