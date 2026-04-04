package org.blackum.blackaddons.gui.widget;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.render.RenderHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BarGraphWidget extends Widget {
    private final String title;
    private final List<BarEntry> entries = new ArrayList<>();
    private double maxValue = 0;
    private String unit = "";

    public BarGraphWidget(int x, int y, int width, String title) {
        super(x, y, width, 0);
        this.title = title;
    }

    public void setData(Map<String, Double> data, String unit) {
        this.entries.clear();
        this.unit = unit;
        this.maxValue = 0;

        for (Map.Entry<String, Double> entry : data.entrySet()) {
            this.entries.add(new BarEntry(entry.getKey(), entry.getValue()));
            if (entry.getValue() > maxValue) {
                this.maxValue = entry.getValue();
            }
        }

        this.height = 30 + (entries.size() * 20);
    }

    private java.util.Map<String, Integer> colorMap = new java.util.HashMap<>();

    public void setColorMap(java.util.Map<String, Integer> colorMap) {
        this.colorMap = colorMap;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible)
            return;

        graphics.text(Minecraft.getInstance().font, ChatFormatting.BOLD + title, x, y, Theme.ACCENT);

        int currentY = y + 20;
        int maxBarWidth = width - 120;
        for (BarEntry entry : entries) {
            graphics.text(Minecraft.getInstance().font, ChatFormatting.WHITE + entry.label, x, currentY + 2,
                    0xFFFFFFFF);

            int barX = x + 60;
            RenderHelper.renderRoundedRect(graphics, barX, currentY + 4, maxBarWidth, 4, 2, Theme.BACKGROUND_TERTIARY);
            if (maxValue > 0) {
                int fillWidth = (int) ((entry.value / maxValue) * maxBarWidth);
                int color = colorMap.getOrDefault(entry.label, Theme.ACCENT_PRIMARY);
                RenderHelper.renderRoundedRect(graphics, barX, currentY + 4, Math.max(4, fillWidth), 4, 2, color);
            }
            String valueText = formatValue(entry.value) + " " + unit;
            graphics.text(Minecraft.getInstance().font, ChatFormatting.GRAY + valueText, barX + maxBarWidth + 5,
                    currentY + 2,
                    0xFFFFFFFF);

            currentY += 20;
        }
    }

    private String formatValue(double value) {
        if (value >= 1_000_000_000)
            return String.format("%.2fB", value / 1_000_000_000);
        if (value >= 1_000_000)
            return String.format("%.2fM", value / 1_000_000);
        if (value >= 1_000)
            return String.format("%.1fk", value / 1_000);

        if (value % 1 == 0)
            return String.format("%.0f", value);

        return String.format("%.2f", value);
    }

    private static class BarEntry {
        String label;
        double value;

        BarEntry(String label, double value) {
            this.label = label;
            this.value = value;
        }
    }
}
