package org.blackum.blackaddons.core.waypoint;

import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.core.config.ConfigManager.WaypointAction;

public class Waypoint {
    public java.util.UUID id = java.util.UUID.randomUUID();
    public java.util.UUID groupId = null;
    public String name;
    public double x;
    public double y;
    public double z;
    public String dimension;
    public boolean enabled;
    public int color;
    public double radius = 1.0;
    public double height = 0.1;
    public WaypointShape shape = WaypointShape.CYLINDER;
    public boolean showFullShape = false;
    public WaypointAnimation animation = WaypointAnimation.STATIC;
    public float reuseCooldownSeconds = 0.0f;
    public java.util.List<WaypointAction> actions = new java.util.ArrayList<>();

    public Waypoint() {
    }

    public Waypoint(String name, double x, double y, double z, String dimension) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimension = dimension;
        this.enabled = true;
        this.color = Theme.ACCENT;
    }
}
