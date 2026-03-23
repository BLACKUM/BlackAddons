package org.blackum.blackaddons.core.waypoint;

import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.core.config.ConfigManager.WaypointAction;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Waypoint {
    public UUID id = UUID.randomUUID();
    public UUID groupId = null;
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
    public List<WaypointAction> actions = new ArrayList<>();

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
