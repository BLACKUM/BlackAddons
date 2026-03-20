package org.blackum.blackaddons.core.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.blackum.blackaddons.core.waypoint.Waypoint;

public class MovementUtils {
    private static final double PERFECT_CENTER_EPSILON = 1.0E-2;
    private static final double ARRIVAL_DISTANCE = 0.012D;
    private static final double SNAP_DISTANCE = 0.03D;
    private static final double MIN_GROUND_SPEED = 0.012D;
    private static final double MAX_GROUND_SPEED = 0.22D;
    private static final double MAX_AIR_SPEED = 0.12D;
    private static final double AIR_CONTROL = 0.35D;
    private static final double CROUCH_MULTIPLIER = 0.3D;

    private static boolean active;
    private static double targetX;
    private static double targetZ;

    public static void alignToCenter(Waypoint waypoint) {
        Minecraft mc = Minecraft.getInstance();
        if (waypoint == null || mc.player == null) return;

        moveToBlock(waypoint.x, waypoint.z);
    }

    public static void moveToBlock(double x, double z) {
        targetX = x;
        targetZ = z;
        active = true;
    }

    public static void tick() {
        if (!active) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (!(mc.player instanceof LocalPlayer player) || mc.level == null) {
            cancel();
            return;
        }

        if (!canMove(player, mc)) {
            return;
        }

        double diffX = targetX - player.getX();
        double diffZ = targetZ - player.getZ();
        double distSq = diffX * diffX + diffZ * diffZ;
        if (distSq <= ARRIVAL_DISTANCE * ARRIVAL_DISTANCE) {
            Vec3 velocity = player.getDeltaMovement();
            if (mc.hasSingleplayerServer()) {
                player.setPos(targetX, player.getY(), targetZ);
            }
            player.setDeltaMovement(0.0D, velocity.y, 0.0D);
            cancel();
            return;
        }

        double dist = Math.sqrt(distSq);
        double dirX = diffX / dist;
        double dirZ = diffZ / dist;
        double speed = player.onGround() ? MAX_GROUND_SPEED : MAX_AIR_SPEED;
        if (player.isCrouching()) {
            speed *= CROUCH_MULTIPLIER;
        }

        Vec3 velocity = player.getDeltaMovement();
        if (player.onGround()) {
            if (dist <= SNAP_DISTANCE && mc.hasSingleplayerServer()) {
                player.setPos(targetX, player.getY(), targetZ);
                player.setDeltaMovement(0.0D, velocity.y, 0.0D);
                cancel();
                return;
            }
            speed = Math.max(MIN_GROUND_SPEED, Math.min(speed, dist * 0.65D));
            player.setDeltaMovement(dirX * speed, velocity.y, dirZ * speed);
            return;
        }

        double nextX = velocity.x * 0.91D + dirX * speed * AIR_CONTROL;
        double nextZ = velocity.z * 0.91D + dirZ * speed * AIR_CONTROL;
        player.setDeltaMovement(nextX, velocity.y, nextZ);
    }

    public static void cancel() {
        active = false;
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean isPerfectlyCentered(Waypoint waypoint) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;

        return Math.abs(mc.player.getX() - waypoint.x) < PERFECT_CENTER_EPSILON
                && Math.abs(mc.player.getZ() - waypoint.z) < PERFECT_CENTER_EPSILON;
    }

    private static boolean canMove(LocalPlayer player, Minecraft mc) {
        if (mc.screen != null) {
            return false;
        }
        if (player.isPassenger() || player.isFallFlying() || player.isSwimming()) {
            return false;
        }
        return !player.onClimbable() && !player.isInWater() && !player.isInLava();
    }
}
