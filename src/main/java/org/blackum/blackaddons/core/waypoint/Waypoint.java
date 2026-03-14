package org.blackum.blackaddons.core.waypoint;

import org.blackum.blackaddons.gui.render.Theme;

public class Waypoint {
    public java.util.UUID id = java.util.UUID.randomUUID();
    public String name;
    public double x;
    public double y;
    public double z;
    public String dimension;
    public boolean enabled;
    public int color;
    public double radius = 1.0;
    public double height = 0.1;
    public WaypointAnimation animation = WaypointAnimation.STATIC;
    public java.util.List<org.blackum.blackaddons.core.config.ConfigManager.WaypointAction> actions = new java.util.ArrayList<>();

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
