package org.blackum.blackaddons.core.waypoint;

public enum WaypointAnimation {
    RADAR("Radar"),
    PULSE("Pulse"),
    STATIC("Static"),
    BOUNCE("Bounce"),
    BREATH("Breath"),
    DOUBLE_RADAR("Double Radar");

    private final String label;

    WaypointAnimation(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
