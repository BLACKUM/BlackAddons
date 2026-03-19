package org.blackum.blackaddons.core.manager;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.ChatFormatting;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.mixin.core.GameRendererAccessor;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RotationManager {
    private static final float STOP_THRESHOLD = 0.05f;
    private static final int OVERLAY_BAR_WIDTH = 100;
    private static final int COLOR_BAR_BG = 0xFF333333;
    private static final int COLOR_BAR_FILL = 0xFF00AAFF;
    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final int COLOR_GRAY = 0xFF999999;
    private static final int WAYPOINT_DOT = 6;

    private static RotationManager instance;
    private final Random random = new Random();

    private float startYaw;
    private float startPitch;
    private float targetYawUnwrapped;
    private float targetPitch;
    
    private float cp1Yaw, cp1Pitch;
    private float cp2Yaw, cp2Pitch;
    
    private float durationTicks;
    private float currentTicks;
    private float pendingSpeedOverride = 0;

    private boolean active;

    private double targetX = Double.NaN;
    private double targetY = Double.NaN;
    private double targetZ = Double.NaN;

    private double lastTargetX = Double.NaN;
    private double lastTargetY = Double.NaN;
    private double lastTargetZ = Double.NaN;

    private List<Vec3> splinePoints = null;
    private int currentSplineIndex = 0;

    private Matrix4f lastProjMatrix = new Matrix4f();
    private Matrix4f lastViewMatrix = new Matrix4f();

    private RotationManager() {
        HudRenderCallback.EVENT.register(this::onFrame);
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    public static RotationManager getInstance() {
        if (instance == null) {
            instance = new RotationManager();
        }
        return instance;
    }

    public void rotateTo(float yaw, float pitch) {
        rotateTo(yaw, pitch, 0);
    }

    public void rotateTo(float yaw, float pitch, float speedOverride) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        this.active = true;
        this.targetX = Double.NaN;
        this.targetY = Double.NaN;
        this.targetZ = Double.NaN;
        this.pendingSpeedOverride = speedOverride > 0 ? speedOverride : 0;
        
        setupBezier(mc, normalizeYaw(yaw), Mth.clamp(pitch, -90f, 90f));
    }

    public void snapToAngle(float yaw, float pitch) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        float jitterYaw = (random.nextFloat() - 0.5f) * 0.01f;
        float jitterPitch = (random.nextFloat() - 0.5f) * 0.01f;
        mc.player.setYRot(normalizeYaw(yaw) + jitterYaw);
        mc.player.setXRot(Mth.clamp(pitch, -90f, 90f) + jitterPitch);
    }

    public void snapToBlock(double x, double y, double z) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        double dx = x + 0.5 - mc.player.getX();
        double dy = y + 0.5 - (mc.player.getY() + mc.player.getEyeHeight());
        double dz = z + 0.5 - mc.player.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        float rawYaw = normalizeYaw((float) Math.toDegrees(Math.atan2(dz, dx)) - 90f);
        float rawPitch = Mth.clamp((float) -Math.toDegrees(Math.atan2(dy, dist)), -90f, 90f);
        float jitterYaw = (random.nextFloat() - 0.5f) * 0.01f;
        float jitterPitch = (random.nextFloat() - 0.5f) * 0.01f;
        mc.player.setYRot(rawYaw + jitterYaw);
        mc.player.setXRot(rawPitch + jitterPitch);
    }

    public void rotateToBlock(double x, double y, double z) {
        rotateToBlock(x, y, z, 0);
    }

    public void rotateToBlock(double x, double y, double z, float speedOverride) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        this.pendingSpeedOverride = speedOverride > 0 ? speedOverride : 0;
        float randX = 0;
        float randY = 0;
        float randZ = 0;
        
        if (ConfigManager.data.rotationHumanizerEnabled) {
            float r = ConfigManager.data.rotationTargetRandomness;
            randX = (random.nextFloat() * 2 - 1) * r;
            randY = (random.nextFloat() * 2 - 1) * r;
            randZ = (random.nextFloat() * 2 - 1) * r;
        }

        double newTargetX = x + 0.5 + randX;
        double newTargetY = y + 0.5 + randY;
        double newTargetZ = z + 0.5 + randZ;

        double checkX = Double.isNaN(this.targetX) ? this.lastTargetX : this.targetX;
        double checkY = Double.isNaN(this.targetY) ? this.lastTargetY : this.targetY;
        double checkZ = Double.isNaN(this.targetZ) ? this.lastTargetZ : this.targetZ;

        if (!Double.isNaN(checkX)) {
            double distSq = Math.pow(checkX - newTargetX, 2) + Math.pow(checkY - newTargetY, 2) + Math.pow(checkZ - newTargetZ, 2);
            if (distSq < 1.0) {
                this.targetX = newTargetX;
                this.targetY = newTargetY;
                this.targetZ = newTargetZ;
                this.lastTargetX = newTargetX;
                this.lastTargetY = newTargetY;
                this.lastTargetZ = newTargetZ;
                updateBlockAngles(mc);

                if (!this.active) {
                    if (this.currentTicks < this.durationTicks) {
                        this.active = true;
                        return;
                    } else {
                        float yawDiff = Math.abs(yawDiff(mc.player.getYRot(), this.targetYawUnwrapped));
                        float pitchDiff = Math.abs(mc.player.getXRot() - this.targetPitch);
                        if (yawDiff < 5.0f && pitchDiff < 5.0f) {
                            this.active = true;
                            this.currentTicks = this.durationTicks;
                            return;
                        }
                    }
                } else {
                    return;
                }
            }
        }

        this.active = true;
        this.targetX = newTargetX;
        this.targetY = newTargetY;
        this.targetZ = newTargetZ;
        this.lastTargetX = newTargetX;
        this.lastTargetY = newTargetY;
        this.lastTargetZ = newTargetZ;

        updateBlockAngles(mc);
        setupBezier(mc, this.targetYawUnwrapped, this.targetPitch);
    }

    public void rotateToSpline(List<Vec3> points) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || points.isEmpty()) return;

        this.splinePoints = new ArrayList<>(points);
        this.currentSplineIndex = 0;
        this.active = true;

        setupNextSplineSegment(mc);
    }

    public void advanceSpline() {
        Minecraft mc = Minecraft.getInstance();
        if (this.splinePoints != null && this.currentSplineIndex < this.splinePoints.size() - 1) {
             this.currentSplineIndex++;
             this.active = true;
             setupNextSplineSegment(mc);
        } else {
             this.active = false;
        }
    }

    public void setSpline(List<Vec3> points, int currentIndex) {
        this.splinePoints = new ArrayList<>(points);
        this.currentSplineIndex = currentIndex;
    }

    public void resumeWithSpline(List<Vec3> points) {
        if (points == null || points.isEmpty()) return;
        this.splinePoints = new ArrayList<>(points);
        this.currentSplineIndex = 0;
        this.active = true;
    }

    public void snapToTarget() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !active) return;
        
        updateBlockAngles(mc);
        
        if (!Double.isNaN(targetYawUnwrapped)) {
            mc.player.setYRot(targetYawUnwrapped);
            mc.player.setXRot(targetPitch);
            this.currentTicks = this.durationTicks;
        }
    }

    public boolean isAtSplineNode() {
        return this.active && this.splinePoints != null && this.currentTicks >= this.durationTicks;
    }

    public boolean isActive() {
        return this.active;
    }

    public void clearSpline() {
        this.splinePoints = null;
        this.active = false;
        this.lastTargetX = Double.NaN;
        this.lastTargetY = Double.NaN;
        this.lastTargetZ = Double.NaN;
        this.targetX = Double.NaN;
        this.targetY = Double.NaN;
        this.targetZ = Double.NaN;
        this.targetYawUnwrapped = Float.NaN;
        this.targetPitch = Float.NaN;
    }

    private void setupNextSplineSegment(Minecraft mc) {
        if (splinePoints == null || currentSplineIndex >= splinePoints.size()) {
            this.active = false;
            return;
        }

        Vec3 nextPoint = splinePoints.get(currentSplineIndex);
        this.targetX = nextPoint.x;
        this.targetY = nextPoint.y;
        this.targetZ = nextPoint.z;

        updateBlockAngles(mc);
        setupBezier(mc, this.targetYawUnwrapped, this.targetPitch);
    }

    private void setupBezier(Minecraft mc, float targetY, float targetP) {
        this.startYaw = mc.player.getYRot();
        this.startPitch = mc.player.getXRot();

        float dy = yawDiff(this.startYaw, targetY);
        float dp = targetP - this.startPitch;

        this.targetYawUnwrapped = this.startYaw + dy;
        this.targetPitch = targetP;

        float distance = (float) Math.sqrt(dy * dy + dp * dp);

        float speed = (this.pendingSpeedOverride > 0)
                ? this.pendingSpeedOverride
                : Math.max(0.1f, ConfigManager.data.rotationSpeed);
        this.pendingSpeedOverride = 0;
        this.durationTicks = Math.max(1.0f, distance / speed);
        
        this.currentTicks = 0.0f;

        if (ConfigManager.data.rotationHumanizerEnabled) {
            float curveStrength = ConfigManager.data.rotationVariance;

            float perpYaw = -dp;
            float perpPitch = dy;
            float perpLen = (float) Math.sqrt(perpYaw * perpYaw + perpPitch * perpPitch);
            
            if (perpLen > 0.001f) {
                perpYaw /= perpLen;
                perpPitch /= perpLen;
            } else {
                perpYaw = 1.0f;
                perpPitch = 0.0f;
            }

            float curveScale = distance * curveStrength * 0.5f;

            float r1 = (random.nextFloat() * 2.0f - 1.0f) * curveScale;
            float r2 = (random.nextFloat() * 2.0f - 1.0f) * curveScale;

            float speedOffset1 = (random.nextFloat() * 0.2f - 0.1f);

            this.cp1Yaw = this.startYaw + dy * (0.33f + speedOffset1) + perpYaw * r1;
            this.cp1Pitch = this.startPitch + dp * (0.33f + speedOffset1) + perpPitch * r1;

            if (this.splinePoints != null && this.currentSplineIndex < this.splinePoints.size() - 1) {
                Vec3 nextNextPoint = this.splinePoints.get(this.currentSplineIndex + 1);
                
                double nextDx = nextNextPoint.x - mc.player.getX();
                double nextDy = nextNextPoint.y - (mc.player.getY() + mc.player.getEyeHeight());
                double nextDz = nextNextPoint.z - mc.player.getZ();
                double nextDist = Math.sqrt(nextDx * nextDx + nextDz * nextDz);

                float nextRawTargetYaw = normalizeYaw((float) Math.toDegrees(Math.atan2(nextDz, nextDx)) - 90f);
                float nextTargetPitch = Mth.clamp((float) -Math.toDegrees(Math.atan2(nextDy, nextDist)), -90f, 90f);

                float nextDyaw = yawDiff(targetY, this.startYaw + yawDiff(this.startYaw, nextRawTargetYaw));
                float nextDpitch = nextTargetPitch - targetP;

                this.cp2Yaw = targetY - nextDyaw * 0.2f + perpYaw * r2;
                this.cp2Pitch = targetP - nextDpitch * 0.2f + perpPitch * r2;
            } else {
                float overshoot = 0.8f + (random.nextFloat() * 0.35f); 
                this.cp2Yaw = this.startYaw + dy * overshoot + perpYaw * r2;
                this.cp2Pitch = this.startPitch + dp * overshoot + perpPitch * r2;
            }
        } else {
            this.cp1Yaw = this.startYaw + dy * 0.333f;
            this.cp1Pitch = this.startPitch + dp * 0.333f;

            if (this.splinePoints != null && this.currentSplineIndex < this.splinePoints.size() - 1) {
                Vec3 nextNextPoint = this.splinePoints.get(this.currentSplineIndex + 1);
                
                double nextDx = nextNextPoint.x - mc.player.getX();
                double nextDy = nextNextPoint.y - (mc.player.getY() + mc.player.getEyeHeight());
                double nextDz = nextNextPoint.z - mc.player.getZ();
                double nextDist = Math.sqrt(nextDx * nextDx + nextDz * nextDz);

                float nextRawTargetYaw = normalizeYaw((float) Math.toDegrees(Math.atan2(nextDz, nextDx)) - 90f);
                float nextTargetPitch = Mth.clamp((float) -Math.toDegrees(Math.atan2(nextDy, nextDist)), -90f, 90f);

                float nextDyaw = yawDiff(targetY, this.startYaw + yawDiff(this.startYaw, nextRawTargetYaw));
                float nextDpitch = nextTargetPitch - targetP;

                this.cp2Yaw = targetY - nextDyaw * 0.2f;
                this.cp2Pitch = targetP - nextDpitch * 0.2f;
            } else {
                this.cp2Yaw = this.startYaw + dy * 0.666f;
                this.cp2Pitch = this.startPitch + dp * 0.666f;
            }
        }
    }

    private void updateBlockAngles(Minecraft mc) {
        if (mc.player == null || Double.isNaN(targetX)) return;
        double dx = targetX - mc.player.getX();
        double dy = targetY - (mc.player.getY() + mc.player.getEyeHeight());
        double dz = targetZ - mc.player.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        
        float rawTargetYaw = normalizeYaw((float) Math.toDegrees(Math.atan2(dz, dx)) - 90f);
        this.targetPitch = Mth.clamp((float) -Math.toDegrees(Math.atan2(dy, dist)), -90f, 90f);
        this.targetYawUnwrapped = this.startYaw + yawDiff(this.startYaw, rawTargetYaw);
    }

    private static float normalizeYaw(float yaw) {
        yaw = yaw % 360f;
        if (yaw > 180f) yaw -= 360f;
        if (yaw < -180f) yaw += 360f;
        return yaw;
    }

    private static float yawDiff(float from, float to) {
        float diff = (to - from) % 360f;
        if (diff > 180f) diff -= 360f;
        if (diff < -180f) diff += 360f;
        return diff;
    }

    private void onTick(Minecraft mc) {
    }

    private void onFrame(GuiGraphics graphics, DeltaTracker tracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (active) {
            if (mc.screen != null) {
                active = false;
            } else {
                float dt = tracker.getGameTimeDeltaTicks();

                if (!Double.isNaN(targetX)) {
                    updateBlockAngles(mc);
                }


                if (ConfigManager.data.rotationHumanizerEnabled && !Double.isNaN(targetX)) {
                    float currentYawDiff = yawDiff(mc.player.getYRot(), targetYawUnwrapped);
                    float currentPitchDiff = targetPitch - mc.player.getXRot();
                    float currentAngDist = (float) Math.sqrt(currentYawDiff * currentYawDiff + currentPitchDiff * currentPitchDiff);

                    if (currentAngDist <= ConfigManager.data.rotationFovSlowdown) {
                        double physicalDist = Math.sqrt(
                                Math.pow(this.targetX - mc.player.getX(), 2) +
                                Math.pow(this.targetY - mc.player.getY(), 2) +
                                Math.pow(this.targetZ - mc.player.getZ(), 2)
                        );

                        float maxRadius = Math.max(1.0f, ConfigManager.data.rotationDistanceRadius);
                        float distRatio = (float) Math.min(1.0, physicalDist / maxRadius);
                        float addedDurationMultiplier = distRatio * ConfigManager.data.rotationDistanceSlowdown;
                        
                        dt /= (1.0f + addedDurationMultiplier);
                    }
                }

                this.currentTicks += dt;

                if (this.currentTicks >= this.durationTicks) {
                    this.currentTicks = this.durationTicks;
                    
                    if (this.splinePoints == null) {
                        this.active = false;
                    }
                }

                float t = this.currentTicks / this.durationTicks;

                float easedT = t;
                if (ConfigManager.data.rotationSmoothness > 0) {
                    float sineInOut = (float) (0.5 * (1 - Math.cos(Math.PI * t)));
                    float expOut = (float) (t == 1.0f ? 1.0f : 1.0f - Math.pow(2.0, -10.0 * t));
                    float combinedEasing = (sineInOut + expOut) * 0.5f;
                    easedT = t + (combinedEasing - t) * ConfigManager.data.rotationSmoothness;
                }

                float u = 1.0f - easedT;
                float tt = easedT * easedT;
                float uu = u * u;
                float uuu = uu * u;
                float ttt = tt * easedT;

                float currentTargetYaw = uuu * startYaw
                                       + 3 * uu * easedT * cp1Yaw
                                       + 3 * u * tt * cp2Yaw
                                       + ttt * targetYawUnwrapped;

                float currentTargetPitch = uuu * startPitch
                                         + 3 * uu * easedT * cp1Pitch
                                         + 3 * u * tt * cp2Pitch
                                         + ttt * targetPitch;

                if (ConfigManager.data.rotationHumanizerEnabled) {
                    float timeSec = (System.currentTimeMillis() % 10000) / 1000.0f;
                    float noiseY = (float) (Math.sin(timeSec * 15.0) * Math.cos(timeSec * 7.0));
                    float noiseP = (float) (Math.cos(timeSec * 13.0) * Math.sin(timeSec * 11.0));
                    
                    float noiseScale = (1.0f - t) * 0.5f; 
                    currentTargetYaw += noiseY * noiseScale;
                    currentTargetPitch += noiseP * noiseScale;
                }

                if (!Float.isNaN(currentTargetYaw) && !Float.isNaN(currentTargetPitch)) {
                    mc.player.setYRot(currentTargetYaw);
                    mc.player.setXRot(currentTargetPitch);
                }
            }
        }

        if (ConfigManager.data.showRotationDebug) {
            renderOverlay(graphics, tracker);
            if (!Double.isNaN(targetX)) {
                renderWaypoint(graphics, tracker);
            }
        }
    }

    private void renderOverlay(GuiGraphics g, DeltaTracker tracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int screenW = mc.getWindow().getGuiScaledWidth();
        int overlayX = ConfigManager.data.rotationOverlayX < 0
                ? screenW - 145
                : ConfigManager.data.rotationOverlayX;
        int screenH = mc.getWindow().getGuiScaledHeight();
        int overlayY = ConfigManager.data.rotationOverlayY < 0
                ? screenH - 80
                : ConfigManager.data.rotationOverlayY;
        int y = overlayY;
        int lineH = 10;

        float curYaw = normalizeYaw(mc.player.getYRot());
        float curPitch = mc.player.getXRot();

        g.drawString(mc.font,
                ChatFormatting.GOLD + "[Rotation] " + (active ? ChatFormatting.GREEN + "ACTIVE" : ChatFormatting.GRAY + "IDLE"),
                overlayX, y, COLOR_WHITE);
        y += lineH;

        if (active) {
            g.drawString(mc.font, String.format("Yaw:   %.1f -> %.1f", curYaw, targetYawUnwrapped), overlayX, y, COLOR_WHITE);
            y += lineH;
            g.drawString(mc.font, String.format("Pitch: %.1f -> %.1f", curPitch, targetPitch), overlayX, y, COLOR_WHITE);
            y += lineH;

            float progress = durationTicks > 0 ? Math.max(0, currentTicks / durationTicks) : 1.0f;

            if (!Double.isNaN(targetX)) {
                g.drawString(mc.font, String.format("Pos: %.0f %.0f %.0f", targetX, targetY, targetZ), overlayX, y, COLOR_GRAY);
                y += lineH;
            }

            String extra = String.format("Spd: %.1f Curve: %.0f%%%s", ConfigManager.data.rotationSpeed, ConfigManager.data.rotationVariance * 100, ConfigManager.data.rotationHumanizerEnabled
                    ? String.format("  Hum: ON")
                    : "");
            g.drawString(mc.font, extra, overlayX, y, COLOR_GRAY);
            y += lineH;

            g.fill(overlayX, y, overlayX + OVERLAY_BAR_WIDTH, y + 4, COLOR_BAR_BG);
            g.fill(overlayX, y, overlayX + (int) (OVERLAY_BAR_WIDTH * progress), y + 4, COLOR_BAR_FILL);
            g.drawString(mc.font, String.format(" %.0f%%", progress * 100), overlayX + OVERLAY_BAR_WIDTH, y - 2, COLOR_GRAY);
        }
    }

    private void renderWaypoint(GuiGraphics g, DeltaTracker tracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameRenderer == null) return;

        Vec3 cam = mc.gameRenderer.getMainCamera().position();
        double relX = targetX - cam.x;
        double relY = targetY - cam.y;
        double relZ = targetZ - cam.z;

        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        
        float partialTicks = tracker.getGameTimeDeltaTicks();
        float fov = (float) Math.toRadians(((GameRendererAccessor) mc.gameRenderer).invokeGetFov(mc.gameRenderer.getMainCamera(), partialTicks, true));
        float aspect = (float) mc.getWindow().getWidth() / (float) mc.getWindow().getHeight();
        Matrix4f proj = new Matrix4f().perspective(fov, aspect, 0.05f, mc.gameRenderer.getRenderDistance() * 4.0f);

        org.joml.Quaternionf camRot = new org.joml.Quaternionf(mc.gameRenderer.getMainCamera().rotation());
        camRot.conjugate();
        Matrix4f view = new Matrix4f().rotation(camRot);
        Vector4f pos = new Vector4f((float) relX, (float) relY, (float) relZ, 1.0f);
        
        pos.mul(view).mul(proj);

        if (pos.w <= 0.0f) return;

        float ndcX = pos.x / pos.w;
        float ndcY = pos.y / pos.w;

        float sx = (ndcX + 1.0f) * 0.5f * screenW;
        float sy = (1.0f - ndcY) * 0.5f * screenH;

        int ix = (int) sx;
        int iy = (int) sy;
        int half = WAYPOINT_DOT / 2;

        ix = Mth.clamp(ix, half + 1, screenW - half - 1);
        iy = Mth.clamp(iy, half + 1, screenH - half - 1);

        int cx = screenW / 2;
        int cy = screenH / 2;
        drawLine2D(g, cx, cy, ix, iy, Theme.withAlpha(Theme.ACCENT, 0.5f));

        g.fill(ix - half, iy - half, ix + half, iy + half, Theme.ACCENT);
        g.fill(ix - half - 1, iy - half - 1, ix + half + 1, iy - half, 0xFFFFFFFF);
        g.fill(ix - half - 1, iy + half, ix + half + 1, iy + half + 1, 0xFFFFFFFF);
        g.fill(ix - half - 1, iy - half - 1, ix - half, iy + half + 1, 0xFFFFFFFF);
        g.fill(ix + half, iy - half - 1, ix + half + 1, iy + half + 1, 0xFFFFFFFF);

        double dist = Math.sqrt(relX * relX + relY * relY + relZ * relZ);
        String label = String.format("%.1fm", dist);
        g.drawString(mc.font, label, ix - mc.font.width(label) / 2, iy + half + 3, Theme.ACCENT);
    }

    private void drawLine2D(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist == 0) return;
        
        float stepX = dx / dist;
        float stepY = dy / dist;
        
        for (int i = 0; i < dist; i += 2) {
            int px = x1 + (int) (stepX * i);
            int py = y1 + (int) (stepY * i);
            g.fill(px, py, px + 1, py + 1, color);
        }
    }

    public static List<String> getDebugInfo() {
        List<String> info = new ArrayList<>();
        if (!ConfigManager.data.showRotationDebug) return info;

        RotationManager rm = getInstance();
        if (rm.active) {
            Minecraft mc = Minecraft.getInstance();
            info.add("");
            info.add(ChatFormatting.GOLD + "[Rotation Manager]");
            info.add("Status: ACTIVE");
            if (mc.player != null) {
                float curYaw = normalizeYaw(mc.player.getYRot());
                info.add(String.format("Yaw: %.2f -> %.2f (diff %.2f°)", curYaw, rm.targetYawUnwrapped, yawDiff(curYaw, rm.targetYawUnwrapped)));
                info.add(String.format("Pitch: %.2f -> %.2f", mc.player.getXRot(), rm.targetPitch));
            }
            if (!Double.isNaN(rm.targetX)) {
                info.add(String.format("Target: %.1f %.1f %.1f", rm.targetX, rm.targetY, rm.targetZ));
            }
            info.add(String.format("Spd: %.1f | Smooth: %.2f | Curve: %.0f%%",
                    ConfigManager.data.rotationSpeed, ConfigManager.data.rotationSmoothness, ConfigManager.data.rotationVariance * 100));
            info.add(String.format("Hum: %s | Ticks: %.1f/%.1f",
                    ConfigManager.data.rotationHumanizerEnabled ? "ON" : "OFF",
                    rm.currentTicks, rm.durationTicks));
        }
        return info;
    }
}
