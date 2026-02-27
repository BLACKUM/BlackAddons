package org.blackum.blackaddons.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.blackum.blackaddons.core.model.SkyblockItem;
import org.blackum.blackaddons.gui.render.RenderHelper;

import java.util.ArrayList;
import java.util.List;

public class ItemGridWidget extends Widget {
    private final List<SkyblockItem> items;
    private final int columns;
    private final float itemScale = org.blackum.blackaddons.gui.render.Theme.ITEM_GRID_SCALE;
    private final int slotSize = (int) (org.blackum.blackaddons.gui.render.Theme.ITEM_GRID_SLOT_SIZE
            * org.blackum.blackaddons.gui.render.Theme.ITEM_GRID_SCALE);
    private final int padding = (int) (org.blackum.blackaddons.gui.render.Theme.ITEM_GRID_PADDING
            * org.blackum.blackaddons.gui.render.Theme.ITEM_GRID_SCALE);

    public enum Alignment {
        LEFT, CENTER
    }

    private Alignment alignment = Alignment.LEFT;
    private java.util.function.Consumer<SkyblockItem> onClick;
    private SkyblockItem selectedItem;

    public ItemGridWidget(int x, int y, int columns, List<SkyblockItem> items) {
        super(x, y,
                columns * (int) (org.blackum.blackaddons.gui.render.Theme.ITEM_GRID_SLOT_SIZE
                        * org.blackum.blackaddons.gui.render.Theme.ITEM_GRID_SCALE)
                        + (int) (org.blackum.blackaddons.gui.render.Theme.ITEM_GRID_PADDING
                                * org.blackum.blackaddons.gui.render.Theme.ITEM_GRID_SCALE) * 2,
                ((items.size() + columns - 1) / columns)
                        * (int) (org.blackum.blackaddons.gui.render.Theme.ITEM_GRID_SLOT_SIZE
                                * org.blackum.blackaddons.gui.render.Theme.ITEM_GRID_SCALE)
                        + (int) (org.blackum.blackaddons.gui.render.Theme.ITEM_GRID_PADDING
                                * org.blackum.blackaddons.gui.render.Theme.ITEM_GRID_SCALE) * 2);
        this.items = items;
        this.columns = columns;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible)
            return;

        int gridWidth = columns * slotSize + padding * 2;
        int renderX = x;
        if (alignment == Alignment.CENTER) {
            renderX = x + (width - gridWidth) / 2;
        }

        for (int i = 0; i < items.size(); i++) {
            int row = i / columns;
            int col = i % columns;
            int slotX = renderX + padding + col * slotSize;
            int slotY = y + padding + row * slotSize;

            SkyblockItem item = items.get(i);
            int bgColor = (item != null && item == selectedItem) ? 0x8800A8FF : 0x44FFFFFF;
            RenderHelper.renderRoundedRect(graphics, slotX, slotY, slotSize - 2, slotSize - 2, 4, bgColor);

            if (item != null && !item.itemStack().isEmpty()) {
                ItemStack stack = item.itemStack();
                graphics.pose().pushMatrix();
                graphics.pose().translate(slotX + (slotSize - 2) / 2.0f, slotY + (slotSize - 2) / 2.0f);
                graphics.pose().scale(itemScale, itemScale);
                graphics.pose().translate(-8, -8);
                graphics.renderItem(stack, 0, 0);
                graphics.renderItemDecorations(Minecraft.getInstance().font, stack, 0, 0);

                if (item.customStackText() != null) {
                    graphics.pose().pushMatrix();
                    String text = item.customStackText();
                    int textWidth = Minecraft.getInstance().font.width(text);
                    int color = item.customStackTextColor() != null ? item.customStackTextColor() : 0xFFFFFF;
                    graphics.drawString(Minecraft.getInstance().font, text, 16 - textWidth, 9, color, true);
                    graphics.pose().popMatrix();
                }

                graphics.pose().popMatrix();
            }
        }
    }

    @Override
    public void renderOverlay(GuiGraphics graphics, int mouseX, int mouseY, int rawMouseX, int rawMouseY,
            float partialTick) {
        if (!visible || !isMouseOver(mouseX, mouseY))
            return;

        int gridWidth = columns * slotSize + padding * 2;
        int renderX = x;
        if (alignment == Alignment.CENTER) {
            renderX = x + (width - gridWidth) / 2;
        }

        for (int i = 0; i < items.size(); i++) {
            int row = i / columns;
            int col = i % columns;
            int slotX = renderX + padding + col * slotSize;
            int slotY = y + padding + row * slotSize;

            if (mouseX >= slotX && mouseX <= slotX + slotSize && mouseY >= slotY && mouseY <= slotY + slotSize) {
                SkyblockItem item = items.get(i);
                if (item != null && !item.itemStack().isEmpty()) {
                    ItemStack stack = item.itemStack();
                    List<ClientTooltipComponent> tooltip = new ArrayList<>();
                    for (Component line : stack.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.EMPTY,
                            Minecraft.getInstance().player, TooltipFlag.Default.NORMAL)) {
                        tooltip.add(ClientTooltipComponent.create(line.getVisualOrderText()));
                    }
                    graphics.renderTooltip(Minecraft.getInstance().font, tooltip, rawMouseX, rawMouseY,
                            DefaultTooltipPositioner.INSTANCE, null);
                }
                break;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !isMouseOver(mouseX, mouseY))
            return false;

        int gridWidth = columns * slotSize + padding * 2;
        int renderX = x;
        if (alignment == Alignment.CENTER) {
            renderX = x + (width - gridWidth) / 2;
        }

        for (int i = 0; i < items.size(); i++) {
            int row = i / columns;
            int col = i % columns;
            int slotX = renderX + padding + col * slotSize;
            int slotY = y + padding + row * slotSize;

            if (mouseX >= slotX && mouseX <= slotX + slotSize && mouseY >= slotY && mouseY <= slotY + slotSize) {
                if (onClick != null) {
                    onClick.accept(items.get(i));
                    return true;
                }
            }
        }
        return false;
    }

    public void setOnClick(java.util.function.Consumer<SkyblockItem> onClick) {
        this.onClick = onClick;
    }

    public void setSelectedItem(SkyblockItem selectedItem) {
        this.selectedItem = selectedItem;
    }

    public void setAlignment(Alignment alignment) {
        this.alignment = alignment;
    }
}
