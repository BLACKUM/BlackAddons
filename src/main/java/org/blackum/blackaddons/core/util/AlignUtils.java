package org.blackum.blackaddons.core.util;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;
import net.minecraft.util.Mth;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.core.manager.RotationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AlignUtils {
    private static final double ALIGN_EPSILON = 1.0E-4D;
    private static final long DEBUG_SAMPLE_DELAY_MS = 500L;
    private static final int SNAP_MOVEMENT_LOCK_TICKS = 2;
    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final int LINE_HEIGHT = 10;
    private static final int DEBUG_STEP_COUNT = 2;

    private static final float DEFAULT_FRICTION = 0.6F;
    private static final float FRICTION_COMBINATION_FACTOR = 0.91F;
    private static final float MOVEMENT_SPEED_MULTIPLIER = 0.21600002F;
    private static final double MOVEMENT_INPUT_FACTOR = 0.9800000190734863D;
    private static final double GROUND_CHECK_OFFSET = 0.5000001D;
    private static final int BINARY_SEARCH_ROUNDS = 3;
    private static final int BINARY_SEARCH_ITERATIONS = 24;
    private static final double BINARY_SEARCH_PHI_RANGE = 2.0D;
    private static final double MIN_MOVEMENT_DISTANCE_SQR = 9.0E-6D;
    private static final double MIN_MOVEMENT_DISTANCE = 0.003D;

    private static boolean active;
    private static double targetX;
    private static double targetZ;
    private static long startTimeMs;
    private static long currentTimeoutMs = 1000L;
    private static boolean doLookAfter;
    private static float alignPostYaw;
    private static float alignPostPitch;
    private static boolean alignUseLookAfterCoords;
    private static double alignLookAtX;
    private static double alignLookAtY;
    private static double alignLookAtZ;

    private static int alignState = 0;
    private static float yaw1 = 0;
    private static float yaw2 = 0;
    private static double tick1PlannedA = 0;
    private static double tick1PlannedF = 0;
    private static int movementLockTicks = 0;
    private static boolean forcedForward = false;
    private static boolean forcedSneak = false;

    private static boolean debugExpectedAvailable;
    private static double debugExpectedX;
    private static double debugExpectedZ;
    private static long debugFinishedAtMs;
    private static long debugSampleAtMs;
    private static boolean debugAwaitingSample;
    private static boolean debugMeasuredAvailable;
    private static double debugMeasuredX;
    private static double debugMeasuredZ;
    private static double debugMeasuredError;
    private static int debugPlannedSteps;
    private static int debugCompletedSteps;
    private static final double[] debugPredictedStepX = new double[DEBUG_STEP_COUNT];
    private static final double[] debugPredictedStepZ = new double[DEBUG_STEP_COUNT];
    private static final double[] debugActualStepX = new double[DEBUG_STEP_COUNT];
    private static final double[] debugActualStepZ = new double[DEBUG_STEP_COUNT];
    private static final double[] debugStepDrift = new double[DEBUG_STEP_COUNT];
    private static final boolean[] debugPredictedStepAvailable = new boolean[DEBUG_STEP_COUNT];
    private static final boolean[] debugActualStepAvailable = new boolean[DEBUG_STEP_COUNT];
    private static int debugPendingStepIndex = -1;
    private static double sessionTotalError = 0.0D;
    private static int sessionAlignCount = 0;

    public static void register() {
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("blackaddons", "align_overlay"), (graphics, deltaTracker) -> renderOverlay(graphics));
    }

    public static void alignToBlock(double x, double z, long timeoutMs, boolean lookAfter, boolean useLookAfterCoords, float postYaw, float postPitch, double lookAtX, double lookAtY, double lookAtZ) {
        targetX = x;
        targetZ = z;
        currentTimeoutMs = timeoutMs;
        doLookAfter = lookAfter;
        alignPostYaw = postYaw;
        alignPostPitch = postPitch;
        alignUseLookAfterCoords = useLookAfterCoords;
        alignLookAtX = lookAtX;
        alignLookAtY = lookAtY;
        alignLookAtZ = lookAtZ;
        if (!active) {
            releaseMovementKeys(Minecraft.getInstance());
            active = true;
            alignState = 0;
            startTimeMs = System.currentTimeMillis();
        } else {
            alignState = 0;
        }
        debugExpectedAvailable = false;
        debugMeasuredAvailable = false;
        debugAwaitingSample = false;
        debugFinishedAtMs = 0L;
        debugSampleAtMs = 0L;
        debugPlannedSteps = 0;
        debugCompletedSteps = 0;
        debugPendingStepIndex = -1;
        for (int i = 0; i < DEBUG_STEP_COUNT; i++) {
            debugPredictedStepAvailable[i] = false;
            debugActualStepAvailable[i] = false;
            debugPredictedStepX[i] = 0.0D;
            debugPredictedStepZ[i] = 0.0D;
            debugActualStepX[i] = 0.0D;
            debugActualStepZ[i] = 0.0D;
            debugStepDrift[i] = 0.0D;
        }
    }

    public static void resetSessionStats() {
        sessionAlignCount = 0;
        sessionTotalError = 0.0D;
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        updateDebugMeasurement(mc);

        if (movementLockTicks > 0) {
            movementLockTicks--;
        }

        if (!active) {
            return;
        }

        if (!(mc.player instanceof LocalPlayer player) || mc.level == null) {
            cancel();
            return;
        }

        if (mc.screen != null || player.isPassenger() || player.isFallFlying() || player.onClimbable() || player.isInWater() || player.isInLava()) {
            cancel();
            return;
        }

        if (System.currentTimeMillis() - startTimeMs > currentTimeoutMs) {
            cancel();
            return;
        }

        if (alignState == 0) {
            if (!player.onGround()) {
                return;
            }

            if (player.isSprinting()) {
                player.setSprinting(false);
                return;
            }

            float slipperiness = getSurfaceSlipperiness(player, mc);
            float f = slipperiness * FRICTION_COMBINATION_FACTOR;
            float speedMultiplier = MOVEMENT_SPEED_MULTIPLIER / (slipperiness * slipperiness * slipperiness);
            double a = (double) player.getSpeed() * (double) speedMultiplier * MOVEMENT_INPUT_FACTOR;
            
            double vx = player.getDeltaMovement().x;
            double vz = player.getDeltaMovement().z;
            
            double[] drift = predictDrift2D(vx, vz, (double) f);
            double predX = player.getX() + drift[0];
            double predZ = player.getZ() + drift[1];
            
            double rx = targetX - predX;
            double rz = targetZ - predZ;
            double L = Math.hypot(rx, rz);
            double phi = Math.toDegrees(Math.atan2(rz, rx)) - 90.0D;
            phi = Mth.wrapDegrees(phi);

            if (L <= ALIGN_EPSILON) {
                finish(mc);
                return;
            }

            double d_walk = predictDrift(a, (double) f);
            if (L > (2.0D * d_walk) - ALIGN_EPSILON) {
                applyMovement(mc, player, (float) phi, false);
                return;
            }

            double bestTheta = 0;
            double bestPhi = phi;
            
            for (int round = 0; round < BINARY_SEARCH_ROUNDS; round++) {
                double tLow = 0, tHigh = 180;
                double unitX = yawUnitX((float)bestPhi);
                double unitZ = yawUnitZ((float)bestPhi);
                for (int i = 0; i < BINARY_SEARCH_ITERATIONS; i++) {
                    double mid = (tLow + tHigh) / 2.0;
                    double[] p = simulateFinalPosition(player.getX(), player.getZ(), vx, vz, a, (double)f, (float)(bestPhi + mid), (float)(bestPhi - mid));
                    double distProg = (p[0] - predX) * unitX + (p[1] - predZ) * unitZ;
                    if (distProg > L) tLow = mid;
                    else tHigh = mid;
                }
                bestTheta = tLow;

                double pLow = bestPhi - BINARY_SEARCH_PHI_RANGE, pHigh = bestPhi + BINARY_SEARCH_PHI_RANGE;
                double latUnitX = yawUnitX((float)(bestPhi + 90));
                double latUnitZ = yawUnitZ((float)(bestPhi + 90));
                for (int i = 0; i < BINARY_SEARCH_ITERATIONS; i++) {
                    double mid = (pLow + pHigh) / 2.0;
                    double[] p = simulateFinalPosition(player.getX(), player.getZ(), vx, vz, a, (double)f, (float)(mid + bestTheta), (float)(mid - bestTheta));
                    double latErr = (p[0] - targetX) * latUnitX + (p[1] - targetZ) * latUnitZ;
                    if (latErr > 0) pHigh = mid;
                    else pLow = mid;
                }
                bestPhi = pLow;
            }

            yaw1 = (float) Mth.wrapDegrees(bestPhi + bestTheta);
            yaw2 = (float) Mth.wrapDegrees(bestPhi - bestTheta);
            tick1PlannedA = a;
            tick1PlannedF = (double) f;

            storeExpectedAlignment(player.getX(), player.getZ(), vx, vz, a, (double) f, yaw1, yaw2);
            alignState = 1;
        }

        if (alignState == 1) {
            alignState = 2;
            applyMovement(mc, player, yaw1, false);
        } else if (alignState == 2) {
            capturePendingStep(player, 0);
            yaw2 = correctSecondYaw(player, mc);
            alignState = 3;
            applyMovement(mc, player, yaw2, false);
        } else if (alignState == 3) {
            capturePendingStep(player, 1);
            finish(mc);
        }
    }

    private static void applyMovement(Minecraft mc, LocalPlayer player, float targetYaw, boolean sneak) {
        applyExactYaw(player, targetYaw);
        forcedForward = true;
        forcedSneak = sneak;
        setKeyState(mc.options.keyUp, true);
        if (sneak) {
            setKeyState(mc.options.keyShift, true);
        } else {
            setKeyState(mc.options.keyShift, false);
        }
    }

    public static void cancel() {
        if (!active) return;
        active = false;
        movementLockTicks = 0;
        releasePressedKeys(Minecraft.getInstance());
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean shouldBlockMovementInput() {
        return active || movementLockTicks > 0;
    }

    public static boolean isAllowedMovementKey(Minecraft mc, KeyMapping keyMapping) {
        if (mc == null || mc.options == null) return false;
        if (keyMapping == mc.options.keyUp) return forcedForward;
        if (keyMapping == mc.options.keyShift) return forcedSneak;
        return false;
    }

    public static List<String> getDebugInfo() {
        List<String> info = new ArrayList<>();
        if (!active && !debugAwaitingSample && !debugExpectedAvailable && !debugMeasuredAvailable) {
            return info;
        }

        info.add(ChatFormatting.GOLD + "[Align Debug]");
        info.add("State: " + (active ? ("ACTIVE/" + alignState) : "IDLE"));
        info.add(String.format(Locale.US, "Target: %.4f %.4f", targetX, targetZ));
        if (debugExpectedAvailable) {
            info.add(String.format(Locale.US, "Expected: %.4f %.4f", debugExpectedX, debugExpectedZ));
            info.add(String.format(Locale.US, "Expected err: %.6f", Math.hypot(targetX - debugExpectedX, targetZ - debugExpectedZ)));
        } else {
            info.add("Expected: n/a");
        }
        info.add("Steps: " + debugCompletedSteps + "/" + debugPlannedSteps);
        for (int i = 0; i < debugPlannedSteps && i < DEBUG_STEP_COUNT; i++) {
            if (debugPredictedStepAvailable[i]) {
                info.add(String.format(Locale.US, "Step %d pred: %.4f %.4f", i + 1, debugPredictedStepX[i], debugPredictedStepZ[i]));
            } else {
                info.add("Step " + (i + 1) + " pred: n/a");
            }
            if (debugActualStepAvailable[i]) {
                info.add(String.format(Locale.US, "Step %d actual: %.4f %.4f", i + 1, debugActualStepX[i], debugActualStepZ[i]));
                info.add(String.format(Locale.US, "Step %d drift: %.6f", i + 1, debugStepDrift[i]));
            } else if (debugPendingStepIndex == i) {
                info.add("Step " + (i + 1) + " actual: pending");
            } else {
                info.add("Step " + (i + 1) + " actual: n/a");
            }
        }
        if (debugAwaitingSample) {
            double remainingMs = Math.max(0L, debugSampleAtMs - System.currentTimeMillis());
            info.add(String.format(Locale.US, "Actual@+1.0s: pending (%.0fms)", remainingMs));
        } else if (debugMeasuredAvailable) {
            info.add(String.format(Locale.US, "Actual@+1.0s: %.4f %.4f", debugMeasuredX, debugMeasuredZ));
            info.add(String.format(Locale.US, "Actual err: %.6f", debugMeasuredError));
            if (debugExpectedAvailable) {
                info.add(String.format(Locale.US, "Math vs actual: %.6f", Math.hypot(debugMeasuredX - debugExpectedX, debugMeasuredZ - debugExpectedZ)));
            }
        } else {
            info.add("Actual@+1.0s: n/a");
        }
        if (sessionAlignCount > 0) {
            info.add(String.format(Locale.US, "Session Avg Err: %.6f (%d)", sessionTotalError / sessionAlignCount, sessionAlignCount));
        }
        return info;
    }

    private static void renderOverlay(GuiGraphicsExtractor graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (!ConfigManager.data.showAlignDebug || mc.options.hideGui) {
            return;
        }

        List<String> info = getDebugInfo();
        if (info.isEmpty()) {
            return;
        }

        int screenW = mc.getWindow().getGuiScaledWidth();
        int overlayX = ConfigManager.data.alignOverlayX < 0
                ? screenW - 190
                : ConfigManager.data.alignOverlayX;
        int overlayY = ConfigManager.data.alignOverlayY;

        int y = overlayY;
        for (String line : info) {
            graphics.text(mc.font, line, overlayX, y, COLOR_WHITE);
            y += LINE_HEIGHT;
        }
    }

    private static float getSurfaceSlipperiness(LocalPlayer player, Minecraft mc) {
        if (mc.level == null) return DEFAULT_FRICTION;
        BlockPos groundPos = BlockPos.containing(player.getX(), player.getY() - GROUND_CHECK_OFFSET, player.getZ());
        return mc.level.getBlockState(groundPos).getBlock().getFriction();
    }

    private static float chooseNearestYaw(float currentYaw, float yawA, float yawB) {
        return Math.abs(yawDiff(currentYaw, yawA)) <= Math.abs(yawDiff(currentYaw, yawB)) ? yawA : yawB;
    }

    private static float yawDiff(float from, float to) {
        float diff = (to - from) % 360.0F;
        if (diff > 180.0F) diff -= 360.0F;
        if (diff < -180.0F) diff += 360.0F;
        return diff;
    }

    private static void applyExactYaw(LocalPlayer player, float targetYaw) {
        float yaw = (float) Mth.wrapDegrees((double) targetYaw);
        player.setYRot(yaw);
        player.setYHeadRot(yaw);
        player.setYBodyRot(yaw);
    }

    private static void finish(Minecraft mc) {
        active = false;
        debugFinishedAtMs = System.currentTimeMillis();
        debugSampleAtMs = debugFinishedAtMs + DEBUG_SAMPLE_DELAY_MS;
        debugAwaitingSample = true;
        releasePressedKeys(mc);
        if (!doLookAfter) {
            movementLockTicks = 0;
            return;
        }
        movementLockTicks = SNAP_MOVEMENT_LOCK_TICKS;
        if (alignUseLookAfterCoords) {
            RotationManager.getInstance().snapToBlock(alignLookAtX, alignLookAtY, alignLookAtZ, 0);
        } else {
            RotationManager.getInstance().snapToAngle(alignPostYaw, alignPostPitch, 0);
        }
    }

    private static void releaseMovementKeys(Minecraft mc) {
        if (mc == null || mc.options == null) return;
        clearForcedMovement();
        setKeyState(mc.options.keyUp, false);
        setKeyState(mc.options.keyDown, false);
        setKeyState(mc.options.keyLeft, false);
        setKeyState(mc.options.keyRight, false);
        setKeyState(mc.options.keyJump, false);
        setKeyState(mc.options.keyShift, false);
        setKeyState(mc.options.keySprint, false);
    }

    private static void releasePressedKeys(Minecraft mc) {
        if (mc == null || mc.options == null) return;
        clearForcedMovement();
        restorePhysicalState(mc.options.keyUp, mc);
        restorePhysicalState(mc.options.keyShift, mc);
        restorePhysicalState(mc.options.keySprint, mc);
    }

    private static void clearForcedMovement() {
        forcedForward = false;
        forcedSneak = false;
    }

    private static void setKeyState(KeyMapping key, boolean pressed) {
        if (key instanceof KeyBindingAccessor accessor) {
            KeyMapping.set(accessor.getBoundKey(), pressed);
            accessor.setBlackaddonsIsDown(pressed);
            accessor.blackaddons$setForced(pressed);
        }
    }

    private static void restorePhysicalState(KeyMapping key, Minecraft mc) {
        if (!(key instanceof KeyBindingAccessor accessor)) return;
        boolean physicalDown = isPhysicalKeyDown(mc, accessor.getBoundKey());
        KeyMapping.set(accessor.getBoundKey(), physicalDown);
        accessor.setBlackaddonsIsDown(physicalDown);
        accessor.blackaddons$setForced(false);
    }

    private static boolean isPhysicalKeyDown(Minecraft mc, InputConstants.Key key) {
        if (mc == null || mc.getWindow() == null || key == null) return false;
        if (key.getType() != InputConstants.Type.KEYSYM) return false;
        return InputConstants.isKeyDown(mc.getWindow(), key.getValue());
    }

    private static float normalizeYaw(float yaw) {
        return (float) Mth.wrapDegrees((double) yaw);
    }

    private static void storeExpectedAlignment(double startX, double startZ, double vx, double vz, double a, double f, float firstYaw, float secondYaw) {
        if (vx * vx + vz * vz < MIN_MOVEMENT_DISTANCE_SQR) { vx = 0; vz = 0; }
        double ax1 = a * yawUnitX(firstYaw);
        double az1 = a * yawUnitZ(firstYaw);
        double vx1 = vx + ax1;
        double vz1 = vz + az1;
        double x1 = startX + vx1;
        double z1 = startZ + vz1;

        double vxm1 = vx1 * f;
        double vzm1 = vz1 * f;
        if (vxm1 * vxm1 + vzm1 * vzm1 < MIN_MOVEMENT_DISTANCE_SQR) { vxm1 = 0; vzm1 = 0; }
        double ax2 = a * yawUnitX(secondYaw);
        double az2 = a * yawUnitZ(secondYaw);
        double vx2 = vxm1 + ax2;
        double vz2 = vzm1 + az2;
        double x2 = x1 + vx2;
        double z2 = z1 + vz2;

        double finalVx = vx2 * f;
        double finalVz = vz2 * f;
        double[] finalDrift = predictDrift2D(finalVx, finalVz, f);
        
        debugExpectedX = x2 + finalDrift[0];
        debugExpectedZ = z2 + finalDrift[1];
        debugExpectedAvailable = true;
        
        debugPredictedStepX[0] = x1;
        debugPredictedStepZ[0] = z1;
        debugPredictedStepX[1] = x2;
        debugPredictedStepZ[1] = z2;
        debugPredictedStepAvailable[0] = true;
        debugPredictedStepAvailable[1] = true;
        debugPlannedSteps = 2;
    }

    private static void updateDebugMeasurement(Minecraft mc) {
        if (!debugAwaitingSample || mc == null || mc.player == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now < debugSampleAtMs) {
            return;
        }
        debugMeasuredX = mc.player.getX();
        debugMeasuredZ = mc.player.getZ();
        debugMeasuredError = Math.hypot(targetX - debugMeasuredX, targetZ - debugMeasuredZ);
        debugMeasuredAvailable = true;
        debugAwaitingSample = false;
        sessionAlignCount++;
        sessionTotalError += debugMeasuredError;
    }

    private static void capturePendingStep(LocalPlayer player, int stepIndex) {
        if (player == null || stepIndex < 0 || stepIndex >= DEBUG_STEP_COUNT) {
            return;
        }
        debugActualStepX[stepIndex] = player.getX();
        debugActualStepZ[stepIndex] = player.getZ();
        debugStepDrift[stepIndex] = Math.hypot(
                debugActualStepX[stepIndex] - debugPredictedStepX[stepIndex],
                debugActualStepZ[stepIndex] - debugPredictedStepZ[stepIndex]
        );
        debugActualStepAvailable[stepIndex] = true;
        debugCompletedSteps = Math.max(debugCompletedSteps, stepIndex + 1);
    }

    private static float correctSecondYaw(LocalPlayer player, Minecraft mc) {
        double realX = player.getX();
        double realZ = player.getZ();
        double realVx = player.getDeltaMovement().x;
        double realVz = player.getDeltaMovement().z;
        double a = tick1PlannedA;
        double f = tick1PlannedF;

        double rx = targetX - realX;
        double rz = targetZ - realZ;
        double L = Math.hypot(rx, rz);
        if (L <= ALIGN_EPSILON) {
            return yaw2;
        }

        double phi = Math.toDegrees(Math.atan2(rz, rx)) - 90.0D;
        phi = Mth.wrapDegrees(phi);

        double pLow = phi - 180.0D;
        double pHigh = phi + 180.0D;
        double latUnitX = yawUnitX((float)(phi + 90));
        double latUnitZ = yawUnitZ((float)(phi + 90));
        for (int i = 0; i < BINARY_SEARCH_ITERATIONS; i++) {
            double mid = (pLow + pHigh) / 2.0;
            double[] p = simulateSingleStep(realX, realZ, realVx, realVz, a, f, (float) mid);
            double latErr = (p[0] - targetX) * latUnitX + (p[1] - targetZ) * latUnitZ;
            if (latErr > 0) pHigh = mid;
            else pLow = mid;
        }
        return (float) Mth.wrapDegrees(pLow);
    }

    private static double[] simulateSingleStep(double startX, double startZ, double vx, double vz, double a, double f, float yaw) {
        double v1x = vx;
        double v1z = vz;
        if (v1x * v1x + v1z * v1z < MIN_MOVEMENT_DISTANCE_SQR) {
            v1x = 0;
            v1z = 0;
        }
        v1x += a * yawUnitX(yaw);
        v1z += a * yawUnitZ(yaw);
        double x1 = startX + v1x;
        double z1 = startZ + v1z;
        double v2x = v1x * f;
        double v2z = v1z * f;
        double[] drift = predictDrift2D(v2x, v2z, f);
        return new double[]{x1 + drift[0], z1 + drift[1]};
    }

    private static double predictDrift(double velocity, double friction) {
        double drift = 0.0D;
        double currentV = velocity;
        while (Math.abs(currentV) >= MIN_MOVEMENT_DISTANCE) {
            drift += currentV;
            currentV *= friction;
        }
        return drift;
    }

    private static double[] predictDrift2D(double vx, double vz, double friction) {
        double driftX = 0.0D;
        double driftZ = 0.0D;
        while (vx * vx + vz * vz >= MIN_MOVEMENT_DISTANCE_SQR) {
            driftX += vx;
            driftZ += vz;
            vx *= friction;
            vz *= friction;
        }
        return new double[]{driftX, driftZ};
    }

    private static double[] simulateFinalPosition(double startX, double startZ, double vx, double vz, double a, double f, float yaw1, float yaw2) {
        double v1x = vx;
        double v1z = vz;
        if (v1x * v1x + v1z * v1z < MIN_MOVEMENT_DISTANCE_SQR) {
            v1x = 0; v1z = 0;
        }
        v1x += a * yawUnitX(yaw1);
        v1z += a * yawUnitZ(yaw1);
        double x1 = startX + v1x;
        double z1 = startZ + v1z;

        double v2x = v1x * f;
        double v2z = v1z * f;
        if (v2x * v2x + v2z * v2z < MIN_MOVEMENT_DISTANCE_SQR) {
            v2x = 0; v2z = 0;
        }
        v2x += a * yawUnitX(yaw2);
        v2z += a * yawUnitZ(yaw2);
        double x2 = x1 + v2x;
        double z2 = z1 + v2z;

        double v3x = v2x * f;
        double v3z = v2z * f;
        double[] drift = predictDrift2D(v3x, v3z, f);
        return new double[]{x2 + drift[0], z2 + drift[1]};
    }

    private static double yawUnitX(float yaw) {
        return (double)(-Mth.sin(yaw * 0.017453292519943295F));
    }

    private static double yawUnitZ(float yaw) {
        return (double)Mth.cos(yaw * 0.017453292519943295F);
    }
}
