package org.blackum.blackaddons.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import org.blackum.blackaddons.core.config.ConfigManager.WaypointAction;
import org.blackum.blackaddons.core.waypoint.Waypoint;
import org.blackum.blackaddons.core.waypoint.WaypointDragState;
import org.blackum.blackaddons.core.waypoint.WaypointGroup;
import org.blackum.blackaddons.core.waypoint.WaypointManager;
import org.blackum.blackaddons.gui.render.RenderHelper;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.screen.BlackAddonsGUI;
import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.gui.screen.WaypointActionEditScreen;
import org.blackum.blackaddons.gui.screen.WaypointEditScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class WaypointCard extends Widget {

    private static final int CARD_PADDING = 12;
    private static final int HANDLE_WIDTH = 12;
    private static final int CONTENT_OFFSET = 16;
    private static final int SWATCH_SIZE = 12;
    private static final int BUTTON_HEIGHT = 18;
    private static final int BUTTON_GAP = 6;
    private static final int ROW_HEIGHT = 16;
    private static final int CARD_HEIGHT = CARD_PADDING * 2 + ROW_HEIGHT * 2 + 12 + BUTTON_HEIGHT;
    private static final int DRAG_THRESHOLD = 5;

    private final Waypoint waypoint;
    private final BlackAddonsGUI screen;
    private final Runnable onChanged;
    private int indent = 0;

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

        editBtn = new Button(0, 0, 50, BUTTON_HEIGHT, "Edit", () -> {
            if (Blackaddons.screenOpener != null) {
                Blackaddons.screenOpener.accept(new WaypointEditScreen(screen, waypoint, saved -> {
                    WaypointManager.getInstance().save();
                    onChanged.run();
                }));
            }
        });

        actionsBtn = new Button(0, 0, 60, BUTTON_HEIGHT, "Actions", () -> {
            List<WaypointAction> actions = waypoint.actions;
            if (actions.isEmpty()) actions.add(new WaypointAction());
            if (Blackaddons.screenOpener != null) {
                Blackaddons.screenOpener.accept(new WaypointActionEditScreen(screen, waypoint, actions.get(0)));
            }
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

    public void setIndent(int indent) {
        this.indent = indent;
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

        int btnY = y + height - CARD_PADDING - BUTTON_HEIGHT;
        int availableWidth = width - CARD_PADDING * 2;
        int btnWidth = (availableWidth - BUTTON_GAP * 2) / 3;

        editBtn.setWidth(btnWidth);
        editBtn.setX(x + CARD_PADDING);
        editBtn.setY(btnY);

        actionsBtn.setWidth(btnWidth);
        actionsBtn.setX(editBtn.getX() + btnWidth + BUTTON_GAP);
        actionsBtn.setY(btnY);

        deleteBtn.setWidth(availableWidth - (btnWidth + BUTTON_GAP) * 2);
        deleteBtn.setX(actionsBtn.getX() + btnWidth + BUTTON_GAP);
        deleteBtn.setY(btnY);
    }

    private boolean isInHandleArea(double mouseX, double mouseY) {
        int handleX = x + CARD_PADDING + indent;
        return mouseX >= handleX && mouseX <= handleX + HANDLE_WIDTH
                && mouseY >= y && mouseY <= y + height;
    }

    private boolean isBeingDragged() {
        return dragState != null && dragState.active && dragState.draggedCard == this;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
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

    private void renderContents(GuiGraphicsExtractor graphics, int renderX, int renderY, int renderWidth, int renderHeight, int mouseX, int mouseY, float partialTick, boolean isGhost) {
        renderDragHandle(graphics, renderX, renderY, renderHeight, mouseX, mouseY, isGhost);

        int swatchX = renderX + CARD_PADDING + CONTENT_OFFSET + indent;
        int swatchY = renderY + CARD_PADDING + (ROW_HEIGHT - SWATCH_SIZE) / 2;
        
        graphics.fill(swatchX - 1, swatchY - 1, swatchX + SWATCH_SIZE + 1, swatchY + SWATCH_SIZE + 1, 0x44FFFFFF);
        graphics.fill(swatchX, swatchY, swatchX + SWATCH_SIZE, swatchY + SWATCH_SIZE, 0xFF000000 | waypoint.color);

        int nameX = swatchX + SWATCH_SIZE + 6;
        int nameColor = 0xFF000000 | waypoint.color;
        
        String nameText = waypoint.name;
        if (nameText.isEmpty()) nameText = "Unnamed Waypoint";
        
        WaypointGroup group = WaypointManager.getInstance().getGroup(waypoint.groupId);
        if (group != null) {
            nameText += " §7(in " + group.name + ")";
        }
        graphics.text(Minecraft.getInstance().font, nameText, nameX, renderY + CARD_PADDING + (ROW_HEIGHT - 8) / 2, nameColor);

        Minecraft mc = Minecraft.getInstance();
        String coords = String.format(Locale.ROOT, "%.0f, %.0f, %.0f", waypoint.x, waypoint.y, waypoint.z);
        String infoText = coords;
        
        if (mc.player != null) {
            double dist = Math.sqrt(Math.pow(waypoint.x - mc.player.getX(), 2) + Math.pow(waypoint.y - mc.player.getY(), 2) + Math.pow(waypoint.z - mc.player.getZ(), 2));
            infoText += " §8• §7" + String.format(Locale.ROOT, "%.0fm", dist);
        }

        graphics.text(mc.font, infoText, swatchX, renderY + CARD_PADDING + ROW_HEIGHT + 4 + (ROW_HEIGHT - 8) / 2, Theme.TEXT_SECONDARY);

        int dividerY = renderY + CARD_PADDING + ROW_HEIGHT * 2 + 6;
        graphics.fill(renderX + CARD_PADDING, dividerY, renderX + renderWidth - CARD_PADDING, dividerY + 1, 0x11FFFFFF);

        for (Widget child : children) {
            child.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
    }

    private void renderDragHandle(GuiGraphicsExtractor graphics, int rx, int ry, int rh, int mx, int my, boolean isGhost) {
        boolean hovered = !isGhost && isInHandleArea(mx, my);
        int dotColor = hovered ? Theme.TEXT_PRIMARY : Theme.withAlpha(Theme.TEXT_SECONDARY, 0.3f);
        int dotSize = 2;
        int gap = 2;
        int hx = rx + CARD_PADDING + indent;
        int textAreaHeight = ROW_HEIGHT * 2 + 4;
        int hy = ry + CARD_PADDING + (textAreaHeight - (2 * gap + 3 * dotSize)) / 2;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 2; c++) {
                int dx = hx + c * (dotSize + gap);
                int dy = hy + r * (dotSize + gap);
                graphics.fill(dx, dy, dx + dotSize, dy + dotSize, dotColor);
            }
        }
    }

    @Override
    public void extractRenderOverlay(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int rawMouseX, int rawMouseY, float partialTick) {
        if (!visible) return;

        if (isBeingDragged()) {
            int gx = (int) (mouseX - dragState.mouseOffsetX);
            int gy = (int) (mouseY - dragState.mouseOffsetY);
            
            graphics.pose().pushMatrix();
            
            RenderHelper.renderSurface(graphics, gx, gy, width, height, Theme.BORDER_RADIUS_SMALL, false);
            renderContents(graphics, gx, gy, width, height, mouseX, mouseY, partialTick, true);
            RenderHelper.renderRoundedOutline(graphics, gx, gy, width, height, Theme.BORDER_RADIUS_SMALL, Theme.ACCENT);
            graphics.pose().popMatrix();
        }

        for (Widget child : children) {
            child.extractRenderOverlay(graphics, mouseX, mouseY, rawMouseX, rawMouseY, partialTick);
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
    public boolean mouseClicked(double mouseX, double mouseY, MouseButtonEvent event) {
        if (!enabled || !visible) return false;
        if (event.button() == 0 && isInHandleArea(mouseX, mouseY)) {
            handlePressed = true;
            handlePressX = mouseX;
            handlePressY = mouseY;
            return true;
        }
        for (int i = children.size() - 1; i >= 0; i--) {
            if (children.get(i).mouseClicked(mouseX, mouseY, event)) return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, MouseButtonEvent event) {
        handlePressed = false;
        if (event.button() == 0 && isBeingDragged()) {
            if (onDropCallback != null) {
                onDropCallback.accept(mouseY);
            }
            return true;
        }
        boolean handled = false;
        for (Widget child : children) {
            if (child.mouseReleased(mouseX, mouseY, event)) handled = true;
        }
        return handled;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, MouseButtonEvent event, double dragX, double dragY) {
        if (handlePressed && !isBeingDragged()) {
            double dist = Math.sqrt(Math.pow(mouseX - handlePressX, 2) + Math.pow(mouseY - handlePressY, 2));
            if (dist > DRAG_THRESHOLD) {
                if (dragState != null) {
                    dragState.onDragStart(waypoint, this, mouseX, mouseY);
                }
                handlePressed = false;
            }
            return true;
        }
        if (isBeingDragged()) {
            if (dragState != null) dragState.update(mouseY);
            return true;
        }
        boolean handled = false;
        for (Widget child : children) {
            if (child.mouseDragged(mouseX, mouseY, event, dragX, dragY)) handled = true;
        }
        return handled;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, MouseButtonEvent event, double scrollX, double scrollY) {
        if (!visible) return false;
        boolean handled = false;
        for (Widget child : children) {
            if (child.mouseScrolled(mouseX, mouseY, event, scrollX, scrollY)) handled = true;
        }
        return handled;
    }
}
