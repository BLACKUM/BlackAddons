package org.blackum.blackaddons.gui.screen.tabs;

import net.minecraft.client.Minecraft;
import org.blackum.blackaddons.core.waypoint.Waypoint;
import org.blackum.blackaddons.core.waypoint.WaypointDragState;
import org.blackum.blackaddons.core.waypoint.WaypointGroup;
import org.blackum.blackaddons.core.waypoint.WaypointManager;

import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.screen.BlackAddonsGUI;
import org.blackum.blackaddons.Blackaddons;
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

        addGroupsRecursively(null, 0);

        WaypointManager mgr = WaypointManager.getInstance();
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
            Minecraft mc = Minecraft.getInstance();
            double x = 0, y = 0, z = 0;
            String dim = null;
            if (mc.player != null && mc.level != null) {
                x = mc.player.getX();
                y = mc.player.getY();
                z = mc.player.getZ();
                dim = mc.level.dimension().identifier().toString();
            }
            final double fx = x, fy = y, fz = z;
            final String fdim = dim;
            if (Blackaddons.screenOpener != null) {
                Blackaddons.screenOpener.accept(new WaypointEditScreen(screen, createWaypoint(fx, fy, fz, fdim, null), wp -> {
                    mgr.addWaypoint(wp);
                    rebuildList();
                }));
            }
        });
        waypointList.addItem(addUngroupedBtn);

        waypointList.setScrollOffset(lastScrollOffset);
    }

    private void addGroupsRecursively(UUID parentId, int level) {
        WaypointManager mgr = WaypointManager.getInstance();
        int indent = level * 20;
        int itemWidth = waypointList.getWidth() - 16;
        Minecraft mc = Minecraft.getInstance();

        for (WaypointGroup group : mgr.getSubGroups(parentId)) {
            WaypointGroupCard groupCard = new WaypointGroupCard(group, screen, this::rebuildList);
            groupCard.setDragState(dragState, (mouseY) -> handleDrop(group, mouseY));
            groupCard.setIndent(indent);
            waypointList.addItem(groupCard);

            if (!group.collapsed) {
                List<Waypoint> groupWaypoints = mgr.getWaypointsForGroup(group.id);
                for (Waypoint wp : groupWaypoints) {
                    WaypointCard card = new WaypointCard(wp, screen, this::rebuildList);
                    card.setDragState(dragState, (mouseY) -> handleDrop(wp, mouseY));
                    card.setIndent(indent + 20);
                    waypointList.addItem(card);
                }

                Button addWpBtn = new Button(0, 0, itemWidth, Theme.BUTTON_HEIGHT, "+ Add Waypoint", () -> {
                    double x = 0, y = 0, z = 0;
                    String dim = null;
                    if (mc.player != null && mc.level != null) {
                        x = mc.player.getX();
                        y = mc.player.getY();
                        z = mc.player.getZ();
                        dim = mc.level.dimension().identifier().toString();
                    }
                    final double fx = x, fy = y, fz = z;
                    final String fdim = dim;
                    final UUID groupId = group.id;
                    if (Blackaddons.screenOpener != null) {
                        Blackaddons.screenOpener.accept(new WaypointEditScreen(screen, createWaypoint(fx, fy, fz, fdim, groupId), wp -> {
                            mgr.addWaypoint(wp);
                            rebuildList();
                        }));
                    }
                });
                
                waypointList.addItem(addWpBtn);

                addGroupsRecursively(group.id, level + 1);
            }
        }
    }

    private void handleDrop(Object dragged, double mouseY) {
        dragState.reset();
        WaypointManager mgr = WaypointManager.getInstance();
        
        Widget targetWidget = null;
        for (Widget widget : waypointList.getItems()) {
            if (mouseY >= widget.getY() && mouseY <= widget.getY() + widget.getHeight()) {
                targetWidget = widget;
                break;
            }
        }
        
        if (dragged instanceof Waypoint) {
            Waypoint wp = (Waypoint) dragged;
            if (targetWidget instanceof WaypointCard) {
                Waypoint targetWp = ((WaypointCard) targetWidget).getWaypoint();
                if (wp == targetWp) return;
                
                wp.groupId = targetWp.groupId;
                mgr.getWaypoints().remove(wp);
                int idx = mgr.getWaypoints().indexOf(targetWp);
                if (idx == -1) {
                    mgr.getWaypoints().add(wp);
                } else {
                    boolean after = mouseY > targetWidget.getY() + targetWidget.getHeight() / 2;
                    mgr.getWaypoints().add(after ? idx + 1 : idx, wp);
                }
            } else if (targetWidget instanceof WaypointGroupCard) {
                wp.groupId = ((WaypointGroupCard) targetWidget).getGroup().id;
                mgr.getWaypoints().remove(wp);
                mgr.getWaypoints().add(0, wp);
            } else {
                wp.groupId = null;
                mgr.getWaypoints().remove(wp);
                mgr.getWaypoints().add(wp);
            }
        } else if (dragged instanceof WaypointGroup) {
            WaypointGroup grp = (WaypointGroup) dragged;
            if (targetWidget instanceof WaypointGroupCard) {
                WaypointGroup targetGrp = ((WaypointGroupCard) targetWidget).getGroup();
                if (grp == targetGrp) return;
                
                double relativeY = (mouseY - targetWidget.getY()) / targetWidget.getHeight();
                
                if (relativeY > 0.25 && relativeY < 0.75 && !isDescendant(grp.id, targetGrp.id)) {
                    grp.parentId = targetGrp.id;
                } else {
                    grp.parentId = targetGrp.parentId;
                    mgr.getGroups().remove(grp);
                    int idx = mgr.getGroups().indexOf(targetGrp);
                    if (idx == -1) {
                        mgr.getGroups().add(grp);
                    } else {
                        boolean after = relativeY >= 0.75;
                        mgr.getGroups().add(after ? idx + 1 : idx, grp);
                    }
                }
            } else if (targetWidget instanceof WaypointCard) {
                Waypoint targetWp = ((WaypointCard) targetWidget).getWaypoint();
                grp.parentId = targetWp.groupId;
                mgr.getGroups().remove(grp);
                mgr.getGroups().add(0, grp);
            } else {
                grp.parentId = null;
                mgr.getGroups().remove(grp);
                mgr.getGroups().add(grp);
            }
        }
        
        mgr.save();
        rebuildList();
    }

    private boolean isDescendant(UUID potentialParent, UUID targetId) {
        return isDescendant(potentialParent, targetId, 0);
    }

    private boolean isDescendant(UUID potentialParent, UUID targetId, int depth) {
        if (depth > 10 || targetId == null) return false;
        if (targetId.equals(potentialParent)) return true;
        WaypointGroup target = WaypointManager.getInstance().getGroup(targetId);
        if (target == null) return false;
        return isDescendant(potentialParent, target.parentId, depth + 1);
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
