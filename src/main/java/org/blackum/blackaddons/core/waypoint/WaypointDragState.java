package org.blackum.blackaddons.core.waypoint;

import org.blackum.blackaddons.gui.widget.Widget;

public class WaypointDragState {
    public boolean active = false;
    public Object draggedItem = null;
    public Widget draggedCard = null;
    public double dragY = 0;
    public double mouseOffsetX = 0;
    public double mouseOffsetY = 0;

    public void onDragStart(Object item, Widget card, double mouseX, double mouseY) {
        this.active = true;
        this.draggedItem = item;
        this.draggedCard = card;
        this.dragY = mouseY;
        this.mouseOffsetX = mouseX - card.getX();
        this.mouseOffsetY = mouseY - card.getY();
    }

    public void update(double mouseY) {
        if (active) {
            this.dragY = mouseY;
        }
    }

    public void reset() {
        active = false;
        draggedItem = null;
        draggedCard = null;
    }
}
