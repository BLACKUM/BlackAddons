package org.blackum.blackaddons.core.waypoint;

public enum WaypointShape {
    CYLINDER("Cylinder"),
    BOX("Box");

    private final String label;

    WaypointShape(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
