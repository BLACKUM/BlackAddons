package org.blackum.blackaddons.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.blackum.blackaddons.core.model.SkyblockItem;
import org.blackum.blackaddons.gui.render.RenderHelper;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.core.util.FormatUtils;
import com.google.gson.JsonObject;

public class PetDetailWidget extends Widget {
    private SkyblockItem pet;

    public PetDetailWidget(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    public void setPet(SkyblockItem pet) {
        this.pet = pet;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible || pet == null)
            return;

        RenderHelper.renderRoundedRect(graphics, x, y, width, height, Theme.BORDER_RADIUS, Theme.SURFACE_LIGHT);
        RenderHelper.renderRoundedOutline(graphics, x, y, width, height, Theme.BORDER_RADIUS, Theme.BORDER);

        int padding = Theme.PADDING_MEDIUM;
        int currentY = y + padding;

        Component name = pet.itemStack().getHoverName();
        graphics.drawString(Minecraft.getInstance().font, name, x + padding, currentY, 0xFFFFFFFF);
        currentY += 20;
        float iconScale = 4.0f;
        graphics.pose().pushMatrix();
        graphics.pose().translate(x + width / 2.0f, currentY + 32);
        graphics.pose().scale(iconScale, iconScale);
        graphics.pose().translate(-8, -8);
        graphics.renderItem(pet.itemStack(), 0, 0);
        graphics.pose().popMatrix();

        currentY += 75;

        String levelPrefix = "Level " + pet.customStackText();
        graphics.drawString(Minecraft.getInstance().font, levelPrefix, x + padding, currentY, 0xFFBBBBBB);
        currentY += 15;
        JsonObject data = pet.extraData();
        int barWidth = width - padding * 2;
        int barHeight = 4;
        RenderHelper.renderRoundedRect(graphics, x + padding, currentY, barWidth, barHeight, 2, 0x44000000);

        if (data != null) {
            String rarity = pet.rarity() != null ? pet.rarity() : "COMMON";
            String type = pet.skyblockId() != null && pet.skyblockId().startsWith("PET_")
                    ? pet.skyblockId().substring(4)
                    : "UNKNOWN";
            long exp = data.has("exp") ? data.get("exp").getAsLong() : 0;

            float progress = org.blackum.blackaddons.core.util.PetUtils.getProgress(type, rarity, exp);
            boolean isMax = progress >= 1.0f;

            if (isMax) {
                org.blackum.blackaddons.gui.render.RenderHelper.renderChromaRect(graphics, x + padding, currentY,
                        barWidth, barHeight);
            } else {
                RenderHelper.renderRoundedRect(graphics, x + padding, currentY, (int) (barWidth * progress), barHeight,
                        2, Theme.ACCENT);
            }
        }
        currentY += 15;

        if (data != null) {
            if (data.has("exp")) {
                graphics.drawString(Minecraft.getInstance().font,
                        "Exp: " + FormatUtils.formatNumber(data.get("exp").getAsDouble()), x + padding, currentY,
                        0xFFAAAAAA);
                currentY += 12;
            }
            if (data.has("heldItem") && !data.get("heldItem").isJsonNull()) {
                graphics.drawString(Minecraft.getInstance().font,
                        "Held Item: §d" + formatPetItem(data.get("heldItem").getAsString()),
                        x + padding, currentY, 0xFFAAAAAA);
                currentY += 12;
            }
            if (data.has("candyUsed")) {
                graphics.drawString(Minecraft.getInstance().font,
                        "Candy Used: " + data.get("candyUsed").getAsInt() + "/10", x + padding, currentY, 0xFFAAAAAA);
                currentY += 12;
            }
        }

        if (isPetMaxLevel(pet)) {
            graphics.drawCenteredString(Minecraft.getInstance().font, "§b§lMAX LEVEL", x + width / 2, y + height - 20,
                    0xFFFFFF);
        }
    }

    private String formatPetItem(String itemId) {
        if (itemId == null || itemId.isEmpty())
            return "None";
        String[] parts = itemId.toLowerCase().replace("pet_item_", "").split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    private boolean isPetMaxLevel(SkyblockItem pet) {
        if (pet == null || pet.customStackText() == null)
            return false;
        String level = pet.customStackText();
        if ("200".equals(level))
            return true;
        if ("100".equals(level) && (pet.skyblockId() == null || !pet.skyblockId().contains("GOLDEN_DRAGON")))
            return true;
        return false;
    }
}
