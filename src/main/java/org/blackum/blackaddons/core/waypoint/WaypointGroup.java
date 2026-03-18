package org.blackum.blackaddons.core.waypoint;

import org.blackum.blackaddons.core.model.DungeonFloor;
import org.blackum.blackaddons.core.util.LocationUtils;

import java.util.UUID;

public class WaypointGroup {

    public UUID id = UUID.randomUUID();
    public String name = "New Group";
    public boolean collapsed = false;
    public boolean enabled = true;

    public String floorFilter = null;
    public Boolean inBossFilter = null;
    public Integer phaseFilter = null;
    public Boolean inDungeonFilter = null;

    public WaypointGroup() {
    }

    public WaypointGroup(String name) {
        this.name = name;
    }

    public boolean isActive() {
        if (!enabled) return false;
        if (inDungeonFilter != null) {
            if (LocationUtils.inDungeons() != inDungeonFilter) {
                return false;
            }
        }
        if (floorFilter != null && !floorFilter.isEmpty()) {
            DungeonFloor floor = LocationUtils.getCurrentFloor();
            if (floor == null || !floor.getDisplayName().equals(floorFilter)) {
                return false;
            }
        }
        if (inBossFilter != null) {
            if (LocationUtils.inBoss() != inBossFilter) {
                return false;
            }
        }
        if (phaseFilter != null && phaseFilter > 0) {
            if (LocationUtils.getF7Phase() != phaseFilter) {
                return false;
            }
        }
        return true;
    }
}
