package org.blackum.blackaddons.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.blackum.blackaddons.core.waypoint.WaypointDragState;
import org.blackum.blackaddons.core.waypoint.WaypointGroup;
import org.blackum.blackaddons.core.waypoint.WaypointManager;
import org.blackum.blackaddons.gui.render.RenderHelper;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.screen.BlackAddonsGUI;
import org.blackum.blackaddons.gui.screen.WaypointGroupEditScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class WaypointGroupCard extends Widget {

    private static final int CARD_HEIGHT = 38;
    private static final int CARD_PADDING = 10;
    private static final int HANDLE_WIDTH = 16;
    private static final int BUTTON_HEIGHT = 22;
    private static final int PILL_HEIGHT = 12;
    private static final int PILL_PADDING_H = 5;
    private static final int DRAG_THRESHOLD = 5;

    private final WaypointGroup group;
    private final BlackAddonsGUI screen;
    private final Runnable onChanged;

    private WaypointDragState dragState;
    private Consumer<Double> onDropCallback;

    private boolean handlePressed = false;
    private double handlePressX = 0;
    private double handlePressY = 0;

    private final ToggleSwitch enabledToggle;
    private final Button editBtn;
    private final Button deleteBtn;
    private final List<Widget> children = new ArrayList<>();

    public WaypointGroupCard(WaypointGroup group, BlackAddonsGUI screen, Runnable onChanged) {
        super(0, 0, 0, CARD_HEIGHT);
        this.group = group;
        this.screen = screen;
        this.onChanged = onChanged;

        enabledToggle = new ToggleSwitch(0, 0, 45, "", "", group.enabled, val -> {
            group.enabled = val;
            WaypointManager.getInstance().save();
        });

        editBtn = new Button(0, 0, 50, BUTTON_HEIGHT, "Edit", () ->
                Minecraft.getInstance().execute(() ->
                        Minecraft.getInstance().setScreen(new WaypointGroupEditScreen(screen, group, onChanged))));

        deleteBtn = new Button(0, 0, 55, BUTTON_HEIGHT, "Delete", () -> {
            WaypointManager.getInstance().removeGroup(group);
            onChanged.run();
        });

        children.add(enabledToggle);
        children.add(editBtn);
        children.add(deleteBtn);
    }

    public void setDragState(WaypointDragState dragState, Consumer<Double> onDropCallback) {
        this.dragState = dragState;
        this.onDropCallback = onDropCallback;
    }

    public WaypointGroup getGroup() {
        return group;
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        layoutChildren();
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        layoutChildren();
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        layoutChildren();
    }

    private void layoutChildren() {
        int btnY = y + (CARD_HEIGHT - BUTTON_HEIGHT) / 2;
        deleteBtn.setX(x + width - CARD_PADDING - deleteBtn.getWidth());
        deleteBtn.setY(btnY);
        editBtn.setX(x + width - CARD_PADDING - deleteBtn.getWidth() - 4 - editBtn.getWidth());
        editBtn.setY(btnY);
        
        int toggleY = y + (CARD_HEIGHT - Theme.TOGGLE_HEIGHT) / 2;
        enabledToggle.setX(editBtn.getX() - 8 - enabledToggle.getWidth());
        enabledToggle.setY(toggleY);
    }

    private boolean isInHandleArea(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + HANDLE_WIDTH + 6
                && mouseY >= y && mouseY <= y + height;
    }

    private boolean isBeingDragged() {
        return dragState != null && dragState.active && dragState.draggedCard == this;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible) return;

        boolean dragged = isBeingDragged();

        if (dragged) {
            RenderHelper.renderRoundedRect(graphics, x, y, width, height, Theme.BORDER_RADIUS_SMALL, Theme.withAlpha(Theme.GLASS_FILL, 0.15f));
            RenderHelper.renderRoundedOutline(graphics, x, y, width, height, Theme.BORDER_RADIUS_SMALL, Theme.withAlpha(Theme.ACCENT, 0.3f));
        } else {
            int fillColor = Theme.withAlpha(Theme.GLASS_FILL, 1.3f);
            RenderHelper.renderRoundedRect(graphics, x, y, width, height, Theme.BORDER_RADIUS_SMALL, fillColor);
            RenderHelper.renderRoundedOutline(graphics, x, y, width, height, Theme.BORDER_RADIUS_SMALL, Theme.ACCENT);
            renderContents(graphics, x, y, width, height, mouseX, mouseY, partialTick, false);
        }

        if (dragState != null && dragState.active && !dragged) {
            double dy = dragState.dragY;
            if (dy >= y - 4 && dy < y + height / 2) {
                graphics.fill(x, y - 1, x + width, y + 1, Theme.ACCENT);
            } else if (dy >= y + height / 2 && dy < y + height + 4) {
                graphics.fill(x, y + height - 1, x + width, y + height + 1, Theme.ACCENT);
            }
        }
    }

    private void renderContents(GuiGraphics graphics, int rx, int ry, int rw, int rh, int mx, int my, float pt, boolean isGhost) {
        renderDragHandle(graphics, rx, ry, rh, mx, my, isGhost);

        String arrow = group.collapsed ? "▶ " : "▼ ";
        int textY = ry + (rh - 8) / 2;
        int contentX = rx + CARD_PADDING + HANDLE_WIDTH + 4;
        graphics.drawString(Minecraft.getInstance().font, arrow, contentX, textY, Theme.TEXT_SECONDARY);

        String name = group.name != null ? group.name : "Group";
        int nameColor = group.enabled ? Theme.ACCENT : Theme.TEXT_SECONDARY;
        graphics.drawString(Minecraft.getInstance().font, name, contentX + 16, textY, nameColor);

        int pillX = contentX + 16 + Minecraft.getInstance().font.width(name) + 8;
        renderPills(graphics, pillX, textY - 2);

        if (!isGhost) {
            for (Widget child : children) {
                child.render(graphics, mx, my, pt);
            }
        }
    }

    private void renderDragHandle(GuiGraphics graphics, int rx, int ry, int rh, int mx, int my, boolean isGhost) {
        boolean hovered = !isGhost && isInHandleArea(mx, my);
        int dotColor = hovered ? Theme.TEXT_PRIMARY : Theme.withAlpha(Theme.TEXT_SECONDARY, 0.5f);
        int dotSize = 2;
        int gap = 3;
        int hx = rx + 6;
        int hy = ry + (rh - (2 * gap + 3 * dotSize)) / 2;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 2; c++) {
                int dx = hx + c * (dotSize + gap);
                int dy = hy + r * (dotSize + gap);
                graphics.fill(dx, dy, dx + dotSize, dy + dotSize, dotColor);
            }
        }
    }

    private void renderPills(GuiGraphics graphics, int startX, int pillY) {
        int px = startX;
        if (group.inDungeonFilter != null) {
            String label = group.inDungeonFilter ? "Dungeon" : "No Dungeon";
            px = drawPill(graphics, px, pillY, label, Theme.withAlpha(0xFF005020, 0.6f));
        }
        if (group.floorFilter != null && !group.floorFilter.isEmpty()) {
            px = drawPill(graphics, px, pillY, group.floorFilter, Theme.withAlpha(Theme.ACCENT, 0.25f));
        }
        if (group.inBossFilter != null) {
            String label = group.inBossFilter ? "Boss" : "No Boss";
            px = drawPill(graphics, px, pillY, label, Theme.withAlpha(0xFF8B0000, 0.5f));
        }
        if (group.phaseFilter != null && group.phaseFilter > 0) {
            drawPill(graphics, px, pillY, "P" + group.phaseFilter, Theme.withAlpha(0xFF004080, 0.5f));
        }
    }

    private int drawPill(GuiGraphics graphics, int px, int py, String text, int bgColor) {
        int textW = Minecraft.getInstance().font.width(text);
        int pillW = textW + PILL_PADDING_H * 2;
        graphics.fill(px, py, px + pillW, py + PILL_HEIGHT, bgColor);
        graphics.fill(px, py, px + pillW, py + 1, Theme.withAlpha(Theme.TEXT_SECONDARY, 0.4f));
        graphics.fill(px, py + PILL_HEIGHT - 1, px + pillW, py + PILL_HEIGHT, Theme.withAlpha(Theme.TEXT_SECONDARY, 0.4f));
        graphics.drawString(Minecraft.getInstance().font, text, px + PILL_PADDING_H, py + (PILL_HEIGHT - 8) / 2, Theme.TEXT_PRIMARY);
        return px + pillW + 4;
    }

    @Override
    public void renderOverlay(GuiGraphics graphics, int mouseX, int mouseY, int rawMouseX, int rawMouseY, float partialTick) {
        if (!visible) return;

        if (isBeingDragged()) {
            int gx = (int) (mouseX - dragState.mouseOffsetX);
            int gy = (int) (mouseY - dragState.mouseOffsetY);
            
            graphics.pose().pushMatrix();
            graphics.pose().translate(0.0f, 0.0f);
            RenderHelper.renderSurface(graphics, gx, gy, width, height, Theme.BORDER_RADIUS_SMALL, false);
            renderContents(graphics, gx, gy, width, height, mouseX, mouseY, partialTick, true);
            RenderHelper.renderRoundedOutline(graphics, gx, gy, width, height, Theme.BORDER_RADIUS_SMALL, Theme.ACCENT);
            graphics.pose().popMatrix();
        }

        for (Widget child : children) {
            child.renderOverlay(graphics, mouseX, mouseY, rawMouseX, rawMouseY, partialTick);
        }
    }

    @Override
    public void updateHoverState(int mouseX, int mouseY) {
        super.updateHoverState(mouseX, mouseY);
        for (Widget child : children) child.updateHoverState(mouseX, mouseY);
    }

    @Override
    public void tick() {
        for (Widget child : children) child.tick();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!enabled || !visible) return false;
        if (button == 0 && isInHandleArea(mouseX, mouseY)) {
            handlePressed = true;
            handlePressX = mouseX;
            handlePressY = mouseY;
            return true;
        }
        for (int i = children.size() - 1; i >= 0; i--) {
            if (children.get(i).mouseClicked(mouseX, mouseY, button)) return true;
        }
        if (isMouseOver(mouseX, mouseY) && button == 0) {
            group.collapsed = !group.collapsed;
            WaypointManager.getInstance().save();
            onChanged.run();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (handlePressed) {
            handlePressed = false;
            if (dragState != null && dragState.active && dragState.draggedCard == this && onDropCallback != null) {
                onDropCallback.accept(mouseY);
            } else if (dragState != null) {
                dragState.reset();
            }
            return true;
        }
        for (Widget child : children) {
            if (child.mouseReleased(mouseX, mouseY, button)) return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (handlePressed) {
            if (dragState != null) {
                if (!dragState.active) {
                    double dx = mouseX - handlePressX;
                    double dy = mouseY - handlePressY;
                    if (Math.sqrt(dx*dx + dy*dy) > DRAG_THRESHOLD) {
                        dragState.onDragStart(group, this, mouseX, mouseY);
                    }
                }
                dragState.update(mouseY);
            }
            return true;
        }
        for (Widget child : children) {
            if (child.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        for (Widget child : children) {
            if (child.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (Widget child : children) {
            if (child.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        for (Widget child : children) {
            if (child.charTyped(character, modifiers)) return true;
        }
        return super.charTyped(character, modifiers);
    }
}
