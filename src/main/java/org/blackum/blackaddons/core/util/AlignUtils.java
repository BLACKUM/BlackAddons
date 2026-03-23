package org.blackum.blackaddons.core.util;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.core.manager.RotationManager;
import org.blackum.blackaddons.mixin.core.KeyBindingAccessor;

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
    private static double debugPendingExpectedX;
    private static double debugPendingExpectedZ;

    public static void register() {
        HudRenderCallback.EVENT.register((graphics, partialTick) -> renderOverlay(graphics));
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

            double vx = player.getDeltaMovement().x;
            double vz = player.getDeltaMovement().z;
            
            float slipperiness = getSurfaceSlipperiness(player, mc);
            double f = slipperiness * 0.91D;
            double a = player.getSpeed() * (0.216D / (slipperiness * slipperiness * slipperiness));
            
            double d_walk = a / (1.0D - f);
            
            double predictedX = player.getX() + vx / (1.0D - f);
            double predictedZ = player.getZ() + vz / (1.0D - f);
            
            double rx = targetX - predictedX;
            double rz = targetZ - predictedZ;
            double L = Math.hypot(rx, rz);
            double phi = Math.toDegrees(Math.atan2(rz, rx)) - 90.0D;
            phi = normalizeYaw((float) phi);

            if (L <= ALIGN_EPSILON) {
                finish(mc);
                return;
            }

            if (L > (2.0D * d_walk) - ALIGN_EPSILON) {
                applyMovement(mc, player, (float) phi, false);
                return;
            }

            double clampRatio = Math.max(-1.0D, Math.min(1.0D, L / (2.0D * d_walk)));
            double theta = Math.toDegrees(Math.acos(clampRatio));
            float candidate1 = normalizeYaw((float) (phi + theta));
            float candidate2 = normalizeYaw((float) (phi - theta));

            yaw1 = chooseNearestYaw(player.getYRot(), candidate1, candidate2);
            yaw2 = normalizeYaw((float) (2.0D * phi - yaw1));
            storeExpectedAlignment(predictedX, predictedZ, d_walk, yaw1, yaw2);

            alignState = 1;
        }

        if (alignState == 1) {
            alignState = 2;
            markPendingStep(0);
            applyMovement(mc, player, yaw1, false);
        } else if (alignState == 2) {
            alignState = 3;
            markPendingStep(1);
            applyMovement(mc, player, yaw2, false);
        } else if (alignState == 3) {
            capturePendingStep(player);
            finish(mc);
        }
    }

    private static void applyMovement(Minecraft mc, LocalPlayer player, float targetYaw, boolean sneak) {
        capturePendingStep(player);
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
            info.add(String.format(Locale.US, "Actual@+0.5s: pending (%.0fms)", remainingMs));
        } else if (debugMeasuredAvailable) {
            info.add(String.format(Locale.US, "Actual@+0.5s: %.4f %.4f", debugMeasuredX, debugMeasuredZ));
            info.add(String.format(Locale.US, "Actual err: %.6f", debugMeasuredError));
            if (debugExpectedAvailable) {
                info.add(String.format(Locale.US, "Math vs actual: %.6f", Math.hypot(debugMeasuredX - debugExpectedX, debugMeasuredZ - debugExpectedZ)));
            }
        } else {
            info.add("Actual@+0.5s: n/a");
        }
        return info;
    }

    private static void renderOverlay(GuiGraphics graphics) {
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
            graphics.drawString(mc.font, line, overlayX, y, COLOR_WHITE);
            y += LINE_HEIGHT;
        }
    }

    private static float getSurfaceSlipperiness(LocalPlayer player, Minecraft mc) {
        if (mc.level == null) return 0.6F;
        BlockPos groundPos = BlockPos.containing(player.getX(), player.getBoundingBox().minY - 0.5000001D, player.getZ());
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
        float yaw = normalizeYaw(targetYaw);
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
    }

    private static void releasePressedKeys(Minecraft mc) {
        if (mc == null || mc.options == null) return;
        clearForcedMovement();
        restorePhysicalState(mc.options.keyUp, mc);
        restorePhysicalState(mc.options.keyShift, mc);
    }

    private static void clearForcedMovement() {
        forcedForward = false;
        forcedSneak = false;
    }

    private static void setKeyState(KeyMapping key, boolean pressed) {
        if (key instanceof KeyBindingAccessor accessor) {
            KeyMapping.set(accessor.getBoundKey(), pressed);
            accessor.setBlackaddonsIsDown(pressed);
        }
    }

    private static void restorePhysicalState(KeyMapping key, Minecraft mc) {
        if (!(key instanceof KeyBindingAccessor accessor)) return;
        boolean physicalDown = isPhysicalKeyDown(mc, accessor.getBoundKey());
        KeyMapping.set(accessor.getBoundKey(), physicalDown);
        accessor.setBlackaddonsIsDown(physicalDown);
    }

    private static boolean isPhysicalKeyDown(Minecraft mc, InputConstants.Key key) {
        if (mc == null || mc.getWindow() == null || key == null) return false;
        if (key.getType() != InputConstants.Type.KEYSYM) return false;
        return InputConstants.isKeyDown(mc.getWindow(), key.getValue());
    }

    private static float normalizeYaw(float yaw) {
        yaw = yaw % 360.0f;
        if (yaw > 180.0f) yaw -= 360.0f;
        if (yaw < -180.0f) yaw += 360.0f;
        return yaw;
    }

    private static void storeExpectedAlignment(double predictedX, double predictedZ, double walkDistance, float firstYaw, float secondYaw) {
        debugExpectedX = predictedX + walkDistance * yawUnitX(firstYaw) + walkDistance * yawUnitX(secondYaw);
        debugExpectedZ = predictedZ + walkDistance * yawUnitZ(firstYaw) + walkDistance * yawUnitZ(secondYaw);
        debugExpectedAvailable = true;
        debugPlannedSteps = 2;
        debugPredictedStepX[0] = predictedX + walkDistance * yawUnitX(firstYaw);
        debugPredictedStepZ[0] = predictedZ + walkDistance * yawUnitZ(firstYaw);
        debugPredictedStepX[1] = debugExpectedX;
        debugPredictedStepZ[1] = debugExpectedZ;
        debugPredictedStepAvailable[0] = true;
        debugPredictedStepAvailable[1] = true;
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
    }

    private static void markPendingStep(int stepIndex) {
        if (stepIndex < 0 || stepIndex >= DEBUG_STEP_COUNT || !debugPredictedStepAvailable[stepIndex]) {
            return;
        }
        debugPendingStepIndex = stepIndex;
        debugPendingExpectedX = debugPredictedStepX[stepIndex];
        debugPendingExpectedZ = debugPredictedStepZ[stepIndex];
    }

    private static void capturePendingStep(LocalPlayer player) {
        if (player == null || debugPendingStepIndex < 0 || debugPendingStepIndex >= DEBUG_STEP_COUNT) {
            return;
        }
        debugActualStepX[debugPendingStepIndex] = player.getX();
        debugActualStepZ[debugPendingStepIndex] = player.getZ();
        debugStepDrift[debugPendingStepIndex] = Math.hypot(
                debugActualStepX[debugPendingStepIndex] - debugPendingExpectedX,
                debugActualStepZ[debugPendingStepIndex] - debugPendingExpectedZ
        );
        debugActualStepAvailable[debugPendingStepIndex] = true;
        debugCompletedSteps = Math.max(debugCompletedSteps, debugPendingStepIndex + 1);
        debugPendingStepIndex = -1;
    }

    private static double yawUnitX(float yaw) {
        double rad = Math.toRadians(yaw);
        return -Math.sin(rad);
    }

    private static double yawUnitZ(float yaw) {
        double rad = Math.toRadians(yaw);
        return Math.cos(rad);
    }
}
