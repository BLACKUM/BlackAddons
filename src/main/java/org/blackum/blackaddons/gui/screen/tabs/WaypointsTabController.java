package org.blackum.blackaddons.gui.screen.tabs;

import net.minecraft.client.Minecraft;
import org.blackum.blackaddons.core.waypoint.Waypoint;
import org.blackum.blackaddons.core.waypoint.WaypointDragState;
import org.blackum.blackaddons.core.waypoint.WaypointGroup;
import org.blackum.blackaddons.core.waypoint.WaypointManager;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.screen.BlackAddonsGUI;
import org.blackum.blackaddons.gui.screen.WaypointEditScreen;
import org.blackum.blackaddons.gui.widget.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WaypointsTabController extends SimpleTabController {

    private static int lastScrollOffset = 0;
    private final WaypointDragState dragState = new WaypointDragState();

    private ListView waypointList;

    public WaypointsTabController(BlackAddonsGUI screen) {
        super(screen);
    }

    @Override
    public void init(TabPanel.Tab waypointsTab) {
        int contentX = waypointsTab.getParent().getContentX();
        int contentY = waypointsTab.getParent().getContentY();
        int contentWidth = waypointsTab.getParent().getContentWidth();
        int contentHeight = waypointsTab.getParent().getContentHeight();

        waypointList = new ListView(contentX + Theme.PADDING, contentY + Theme.PADDING, contentWidth - Theme.PADDING * 2, contentHeight - Theme.PADDING * 2);
        waypointList.setScrollOffset(lastScrollOffset);
        waypointsTab.addWidget(waypointList);

        rebuildList();
    }

    private void rebuildList() {
        if (waypointList == null) return;

        lastScrollOffset = waypointList.getScrollOffset();
        waypointList.clearItems();

        int itemWidth = waypointList.getWidth() - 16;

        Button addGroupBtn = new Button(0, 0, itemWidth, Theme.BUTTON_HEIGHT, "+ Add Group", () -> {
            WaypointGroup group = new WaypointGroup("New Group");
            WaypointManager.getInstance().addGroup(group);
            rebuildList();
        });
        waypointList.addItem(addGroupBtn);

        WaypointManager mgr = WaypointManager.getInstance();
        Minecraft mc = Minecraft.getInstance();

        for (WaypointGroup group : mgr.getGroups()) {
            WaypointGroupCard groupCard = new WaypointGroupCard(group, screen, this::rebuildList);
            groupCard.setDragState(dragState, (mouseY) -> handleDrop(group, mouseY));
            waypointList.addItem(groupCard);

            if (!group.collapsed) {
                List<Waypoint> groupWaypoints = mgr.getWaypointsForGroup(group.id);
                for (Waypoint wp : groupWaypoints) {
                    WaypointCard card = new WaypointCard(wp, screen, this::rebuildList);
                    card.setDragState(dragState, (mouseY) -> handleDrop(wp, mouseY));
                    waypointList.addItem(card);
                }

                Button addWpBtn = new Button(0, 0, itemWidth, Theme.BUTTON_HEIGHT, "+ Add Waypoint", () -> {
                    double x = 0, y = 0, z = 0;
                    String dim = null;
                    if (mc.player != null && mc.level != null) {
                        x = mc.player.getX();
                        y = mc.player.getY();
                        z = mc.player.getZ();
                        dim = mc.level.dimension().location().toString();
                    }
                    final double fx = x, fy = y, fz = z;
                    final String fdim = dim;
                    final UUID groupId = group.id;
                    mc.execute(() -> mc.setScreen(new WaypointEditScreen(screen, createWaypoint(fx, fy, fz, fdim, groupId), wp -> {
                        mgr.addWaypoint(wp);
                        rebuildList();
                    })));
                });
                waypointList.addItem(addWpBtn);
            }
        }

        List<Waypoint> ungrouped = mgr.getWaypointsForGroup(null);
        if (!ungrouped.isEmpty()) {
            SectionHeader ungroupedHeader = new SectionHeader(itemWidth, "Ungrouped");
            waypointList.addItem(ungroupedHeader);
            for (Waypoint wp : ungrouped) {
                WaypointCard card = new WaypointCard(wp, screen, this::rebuildList);
                card.setDragState(dragState, (mouseY) -> handleDrop(wp, mouseY));
                waypointList.addItem(card);
            }
        }

        Button addUngroupedBtn = new Button(0, 0, itemWidth, Theme.BUTTON_HEIGHT, "+ Add Waypoint (Ungrouped)", () -> {
            double x = 0, y = 0, z = 0;
            String dim = null;
            if (mc.player != null && mc.level != null) {
                x = mc.player.getX();
                y = mc.player.getY();
                z = mc.player.getZ();
                dim = mc.level.dimension().location().toString();
            }
            final double fx = x, fy = y, fz = z;
            final String fdim = dim;
            mc.execute(() -> mc.setScreen(new WaypointEditScreen(screen, createWaypoint(fx, fy, fz, fdim, null), wp -> {
                mgr.addWaypoint(wp);
                rebuildList();
            })));
        });
        waypointList.addItem(addUngroupedBtn);

        waypointList.setScrollOffset(lastScrollOffset);
    }

    private void handleDrop(Object dragged, double mouseY) {
        dragState.reset();
        WaypointManager mgr = WaypointManager.getInstance();
        
        Object target = null;
        boolean after = false;
        
        for (Widget widget : waypointList.getItems()) {
            if (mouseY >= widget.getY() && mouseY <= widget.getY() + widget.getHeight()) {
                if (widget instanceof WaypointCard) {
                    target = ((WaypointCard) widget).getWaypoint();
                    after = mouseY > widget.getY() + widget.getHeight() / 2;
                } else if (widget instanceof WaypointGroupCard) {
                    target = ((WaypointGroupCard) widget).getGroup();
                    after = mouseY > widget.getY() + widget.getHeight() / 2;
                }
                break;
            }
        }
        
        if (dragged instanceof Waypoint) {
            Waypoint wp = (Waypoint) dragged;
            if (target instanceof Waypoint) {
                Waypoint targetWp = (Waypoint) target;
                wp.groupId = targetWp.groupId;
                mgr.getWaypoints().remove(wp);
                int idx = mgr.getWaypoints().indexOf(targetWp);
                mgr.getWaypoints().add(after ? idx + 1 : idx, wp);
            } else if (target instanceof WaypointGroup) {
                wp.groupId = ((WaypointGroup) target).id;
                mgr.getWaypoints().remove(wp);
                mgr.getWaypoints().add(0, wp);
            } else {
                mgr.getWaypoints().remove(wp);
                mgr.getWaypoints().add(wp);
            }
        } else if (dragged instanceof WaypointGroup) {
            WaypointGroup grp = (WaypointGroup) dragged;
            WaypointGroup targetGrp = null;
            if (target instanceof WaypointGroup) {
                targetGrp = (WaypointGroup) target;
            } else if (target instanceof Waypoint) {
                targetGrp = mgr.getGroup(((Waypoint) target).groupId);
            }
            
            if (targetGrp != null && targetGrp != grp) {
                mgr.getGroups().remove(grp);
                int idx = mgr.getGroups().indexOf(targetGrp);
                mgr.getGroups().add(after ? idx + 1 : idx, grp);
            }
        }
        
        mgr.save();
        rebuildList();
    }

    private static Waypoint createWaypoint(double x, double y, double z, String dim, UUID groupId) {
        Waypoint wp = new Waypoint("", x, y, z, dim);
        wp.groupId = groupId;
        return wp;
    }

    @Override
    public void tick() {
        if (waypointList != null) {
            lastScrollOffset = waypointList.getScrollOffset();
        }
    }
}
