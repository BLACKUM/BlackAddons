package org.blackum.blackaddons.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.blackum.blackaddons.core.config.ConfigManager.WaypointAction;
import org.blackum.blackaddons.core.waypoint.Waypoint;
import org.blackum.blackaddons.core.waypoint.WaypointDragState;
import org.blackum.blackaddons.core.waypoint.WaypointManager;
import org.blackum.blackaddons.gui.render.RenderHelper;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.screen.BlackAddonsGUI;
import org.blackum.blackaddons.gui.screen.WaypointActionEditScreen;
import org.blackum.blackaddons.gui.screen.WaypointEditScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class WaypointCard extends Widget {

    private static final int CARD_PADDING = 10;
    private static final int HANDLE_WIDTH = 16;
    private static final int CONTENT_OFFSET = HANDLE_WIDTH + 4;
    private static final int SWATCH_SIZE = 10;
    private static final int BUTTON_HEIGHT = 22;
    private static final int BUTTON_GAP = 4;
    private static final int ROW_HEIGHT = 18;
    private static final int CARD_HEIGHT = CARD_PADDING * 2 + ROW_HEIGHT * 2 + 6 + BUTTON_HEIGHT;
    private static final int DRAG_THRESHOLD = 5;

    private final Waypoint waypoint;
    private final BlackAddonsGUI screen;
    private final Runnable onChanged;

    private WaypointDragState dragState;
    private Consumer<Double> onDropCallback;

    private boolean handlePressed = false;
    private double handlePressX = 0;
    private double handlePressY = 0;

    private final ToggleSwitch enabledToggle;
    private final Button editBtn;
    private final Button actionsBtn;
    private final Button deleteBtn;
    private final List<Widget> children = new ArrayList<>();

    public WaypointCard(Waypoint waypoint, BlackAddonsGUI screen, Runnable onChanged) {
        super(0, 0, 0, CARD_HEIGHT);
        this.waypoint = waypoint;
        this.screen = screen;
        this.onChanged = onChanged;

        enabledToggle = new ToggleSwitch(0, 0, 50, "", "", waypoint.enabled, val -> {
            waypoint.enabled = val;
            WaypointManager.getInstance().save();
        });

        editBtn = new Button(0, 0, 50, BUTTON_HEIGHT, "Edit", () ->
                Minecraft.getInstance().execute(() ->
                        Minecraft.getInstance().setScreen(new WaypointEditScreen(screen, waypoint, saved -> {
                            WaypointManager.getInstance().save();
                            onChanged.run();
                        }))));

        actionsBtn = new Button(0, 0, 60, BUTTON_HEIGHT, "Actions", () -> {
            List<WaypointAction> actions = waypoint.actions;
            if (actions.isEmpty()) actions.add(new WaypointAction());
            Minecraft.getInstance().execute(() ->
                    Minecraft.getInstance().setScreen(new WaypointActionEditScreen(screen, waypoint, actions.get(0))));
        });

        deleteBtn = new Button(0, 0, 55, BUTTON_HEIGHT, "Delete", () -> {
            WaypointManager.getInstance().removeWaypoint(waypoint);
            onChanged.run();
        });

        children.add(enabledToggle);
        children.add(editBtn);
        children.add(actionsBtn);
        children.add(deleteBtn);
    }

    public void setDragState(WaypointDragState dragState, Consumer<Double> onDropCallback) {
        this.dragState = dragState;
        this.onDropCallback = onDropCallback;
    }

    public Waypoint getWaypoint() {
        return waypoint;
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
        int toggleY = y + CARD_PADDING + (ROW_HEIGHT - Theme.TOGGLE_HEIGHT) / 2;
        enabledToggle.setX(x + width - CARD_PADDING - enabledToggle.getWidth());
        enabledToggle.setY(toggleY);

        int btnY = y + CARD_PADDING + ROW_HEIGHT * 2 + 6;
        int btnX = x + CARD_PADDING + CONTENT_OFFSET;
        editBtn.setX(btnX);
        editBtn.setY(btnY);
        btnX += editBtn.getWidth() + BUTTON_GAP;
        actionsBtn.setX(btnX);
        actionsBtn.setY(btnY);
        btnX += actionsBtn.getWidth() + BUTTON_GAP;
        deleteBtn.setX(btnX);
        deleteBtn.setY(btnY);
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
            RenderHelper.renderSurface(graphics, x, y, width, height, Theme.BORDER_RADIUS_SMALL, false);
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

    private void renderContents(GuiGraphics graphics, int renderX, int renderY, int renderWidth, int renderHeight, int mouseX, int mouseY, float partialTick, boolean isGhost) {
        renderDragHandle(graphics, renderX, renderY, renderHeight, mouseX, mouseY, isGhost);

        int swatchX = renderX + CARD_PADDING + CONTENT_OFFSET;
        int swatchY = renderY + CARD_PADDING + (ROW_HEIGHT - SWATCH_SIZE) / 2;
        graphics.fill(swatchX, swatchY, swatchX + SWATCH_SIZE, swatchY + SWATCH_SIZE, 0xFF000000 | waypoint.color);

        int nameX = swatchX + SWATCH_SIZE + 6;
        int nameColor = 0xFF000000 | waypoint.color;
        graphics.drawString(Minecraft.getInstance().font, waypoint.name, nameX, renderY + CARD_PADDING + (ROW_HEIGHT - 8) / 2, nameColor);

        String coords = String.format(Locale.ROOT, "%.0f, %.0f, %.0f", waypoint.x, waypoint.y, waypoint.z);
        graphics.drawString(Minecraft.getInstance().font, coords, nameX, renderY + CARD_PADDING + ROW_HEIGHT + 3 + (ROW_HEIGHT - 8) / 2, Theme.TEXT_SECONDARY);

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            double dist = Math.sqrt(Math.pow(waypoint.x - mc.player.getX(), 2) + Math.pow(waypoint.y - mc.player.getY(), 2) + Math.pow(waypoint.z - mc.player.getZ(), 2));
            String distLabel = String.format(Locale.ROOT, "%.0fm", dist);
            int distWidth = mc.font.width(distLabel);
            int distX = renderX + renderWidth - CARD_PADDING - enabledToggle.getWidth() - 6 - distWidth;
            graphics.drawString(mc.font, distLabel, distX, renderY + CARD_PADDING + (ROW_HEIGHT - 8) / 2, Theme.TEXT_SECONDARY);
        }

        if (!isGhost) {
            for (Widget child : children) {
                child.render(graphics, mouseX, mouseY, partialTick);
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
        for (Widget child : children) {
            child.updateHoverState(mouseX, mouseY);
        }
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
                        dragState.onDragStart(waypoint, this, mouseX, mouseY);
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
