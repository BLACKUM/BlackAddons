package org.blackum.blackaddons.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.render.RenderHelper;
import org.blackum.blackaddons.feature.chat.EmojiUtils;

import java.util.Map;
import java.util.function.Consumer;

public class EmojiWidget extends Widget {
    private final Consumer<String> onSelect;
    private static final int BTN_SIZE = 20;
    private static final int PADDING = 4;
    private static final int COLUMNS = 8;

    public EmojiWidget(int x, int y, Consumer<String> onSelect) {
        super(x, y, (BTN_SIZE + PADDING) * COLUMNS + PADDING, 120);
        this.onSelect = onSelect;
        this.visible = false;

        int rows = (int) Math.ceil(EmojiUtils.getEmojiMap().size() / (double) COLUMNS);
        this.height = rows * (BTN_SIZE + PADDING) + PADDING;

        this.y -= this.height;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible)
            return;

        RenderHelper.renderSurface(graphics, x, y, width, height, Theme.BORDER_RADIUS_SMALL, false);

        int currentX = x + PADDING;
        int currentY = y + PADDING;
        int count = 0;

        for (Map.Entry<String, String> entry : EmojiUtils.getEmojiMap().entrySet()) {
            boolean isHovered = mouseX >= currentX && mouseX <= currentX + BTN_SIZE &&
                    mouseY >= currentY && mouseY <= currentY + BTN_SIZE;

            if (isHovered) {
                graphics.fill(currentX, currentY, currentX + BTN_SIZE, currentY + BTN_SIZE, 0x44FFFFFF);
            }

            graphics.text(Minecraft.getInstance().font, entry.getValue(),
                    currentX + (BTN_SIZE - 8) / 2, currentY + (BTN_SIZE - 8) / 2, Theme.TEXT_PRIMARY);

            count++;
            if (count % COLUMNS == 0) {
                currentX = x + PADDING;
                currentY += BTN_SIZE + PADDING;
            } else {
                currentX += BTN_SIZE + PADDING;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, MouseButtonEvent event) {
        if (!visible || !isMouseOver(mouseX, mouseY))
            return false;

        int currentX = x + PADDING;
        int currentY = y + PADDING;
        int count = 0;

        for (Map.Entry<String, String> entry : EmojiUtils.getEmojiMap().entrySet()) {
            if (mouseX >= currentX && mouseX <= currentX + BTN_SIZE &&
                    mouseY >= currentY && mouseY <= currentY + BTN_SIZE) {
                onSelect.accept(entry.getKey());
                return true;
            }

            count++;
            if (count % COLUMNS == 0) {
                currentX = x + PADDING;
                currentY += BTN_SIZE + PADDING;
            } else {
                currentX += BTN_SIZE + PADDING;
            }
        }
        return true;
    }
}
