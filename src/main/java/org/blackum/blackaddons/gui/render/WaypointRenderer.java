package org.blackum.blackaddons.gui.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Brightness;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.client.render.BlackaddonsRenderTypes;
import org.blackum.blackaddons.core.waypoint.Waypoint;
import org.blackum.blackaddons.core.waypoint.WaypointAnimation;
import org.blackum.blackaddons.core.waypoint.WaypointGroup;
import org.blackum.blackaddons.core.waypoint.WaypointManager;
import org.joml.Matrix4f;

import java.awt.Color;

public class WaypointRenderer {

    private static final int CIRCLE_SEGMENTS = 64;

    public static void render(Matrix4f matrix, MultiBufferSource bufferSource, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Vec3 camPos = mc.gameRenderer.getMainCamera().position();

        int count = 0;
        for (Waypoint waypoint : WaypointManager.getInstance().getWaypoints()) {
            if (!waypoint.enabled) continue;
            if (waypoint.groupId != null) {
                WaypointGroup group = WaypointManager.getInstance().getGroup(waypoint.groupId);
                if (group != null && !group.isActive()) continue;
            }
            if (waypoint.dimension != null && !waypoint.dimension.equals(mc.level.dimension().identifier().toString())) continue;
            renderWaypoint(matrix, bufferSource, waypoint, camPos);
        }
        if (count > 0 && System.currentTimeMillis() % 5000 < 50) {
            Blackaddons.LOGGER.info("Rendering {} waypoints", count);
        }
    }

    private static void renderWaypoint(Matrix4f matrix, MultiBufferSource bufferSource, Waypoint waypoint, Vec3 camPos) {
        double x = waypoint.x - camPos.x;
        double y = waypoint.y - camPos.y;
        double z = waypoint.z - camPos.z;

        float radius = (float) waypoint.radius;
        Color color = new Color(waypoint.color, true);

        WaypointAnimation anim = waypoint.animation != null ? waypoint.animation : WaypointAnimation.STATIC;
        renderAnimatedWaypoint(matrix, bufferSource, x, y, z, radius, color, waypoint.height, waypoint, anim);
    }

    private static void renderAnimatedWaypoint(Matrix4f matrix, MultiBufferSource bufferSource, double x, double y, double z, float radius, Color color, double height, Waypoint waypoint, WaypointAnimation animation) {
        VertexConsumer buffer = bufferSource.getBuffer(BlackaddonsRenderTypes.getWaypoint());

        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = color.getAlpha() / 255f;

        float ringHeight = 0.05f;

        switch (animation) {
            case RADAR:
                drawShape(matrix, buffer, waypoint, x, y, z, radius, 0.02f, ringHeight, r, g, b, a * 0.8f, true);
                if (waypoint.showFullShape) {
                    drawShape(matrix, buffer, waypoint, x, y, z, radius, 0.01f, height, r, g, b, a * 0.2f, false);
                    drawShape(matrix, buffer, waypoint, x, y + height, z, radius, 0.02f, ringHeight, r, g, b, a * 0.8f, true);
                }
                float radarTime = (System.currentTimeMillis() % 1500) / 1500f;
                float waveRadius = radius * radarTime;
                if (waveRadius > 0.05f) {
                    drawShape(matrix, buffer, waypoint, x, y, z, waveRadius, 0.03f, ringHeight * 0.5f, r, g, b, a * (1.0f - radarTime), true);
                }
                break;
            case PULSE:
                float pulse = Mth.sin((System.currentTimeMillis() % 2000) / 2000f * (float) Math.PI * 2) * 0.1f + 0.9f;
                float currentRadius = radius * pulse;
                drawShape(matrix, buffer, waypoint, x, y, z, currentRadius, 0.05f, ringHeight, r, g, b, a, true);
                if (waypoint.showFullShape) {
                    drawShape(matrix, buffer, waypoint, x, y, z, currentRadius, 0.02f, height, r, g, b, a * 0.3f, false);
                    drawShape(matrix, buffer, waypoint, x, y + height, z, currentRadius, 0.05f, ringHeight, r, g, b, a, true);
                }
                break;
            case STATIC:
                drawShape(matrix, buffer, waypoint, x, y, z, radius, 0.05f, ringHeight, r, g, b, a, true);
                if (waypoint.showFullShape) {
                    drawShape(matrix, buffer, waypoint, x, y, z, radius, 0.02f, height, r, g, b, a * 0.3f, false);
                    drawShape(matrix, buffer, waypoint, x, y + height, z, radius, 0.05f, ringHeight, r, g, b, a, true);
                }
                break;
            case BOUNCE:
                float bounce = Mth.sin((System.currentTimeMillis() % 1000) / 1000f * (float) Math.PI * 2) * 0.2f;
                drawShape(matrix, buffer, waypoint, x, y + bounce, z, radius, 0.05f, ringHeight, r, g, b, a, true);
                if (waypoint.showFullShape) {
                    drawShape(matrix, buffer, waypoint, x, y + bounce, z, radius, 0.02f, height, r, g, b, a * 0.3f, false);
                    drawShape(matrix, buffer, waypoint, x, y + bounce + height, z, radius, 0.05f, ringHeight, r, g, b, a, true);
                }
                break;
            case BREATH:
                float breath = Mth.sin((System.currentTimeMillis() % 3000) / 3000f * (float) Math.PI * 2) * 0.4f + 0.6f;
                float breathAlpha = a * breath;
                drawShape(matrix, buffer, waypoint, x, y, z, radius, 0.05f, ringHeight, r, g, b, breathAlpha, true);
                if (waypoint.showFullShape) {
                    drawShape(matrix, buffer, waypoint, x, y, z, radius, 0.02f, height, r, g, b, breathAlpha * 0.3f, false);
                    drawShape(matrix, buffer, waypoint, x, y + height, z, radius, 0.05f, ringHeight, r, g, b, breathAlpha, true);
                }
                break;
            case DOUBLE_RADAR:
                drawShape(matrix, buffer, waypoint, x, y, z, radius, 0.02f, ringHeight, r, g, b, a * 0.6f, true);
                if (waypoint.showFullShape) {
                    drawShape(matrix, buffer, waypoint, x, y, z, radius, 0.01f, height, r, g, b, a * 0.2f, false);
                    drawShape(matrix, buffer, waypoint, x, y + height, z, radius, 0.02f, ringHeight, r, g, b, a * 0.6f, true);
                }
                float time1 = (System.currentTimeMillis() % 2000) / 2000f;
                float time2 = ((System.currentTimeMillis() + 1000) % 2000) / 2000f;
                drawShape(matrix, buffer, waypoint, x, y, z, radius * time1, 0.03f, ringHeight * 0.8f, r, g, b, a * (1.0f - time1), true);
                drawShape(matrix, buffer, waypoint, x, y, z, radius * time2, 0.03f, ringHeight * 0.8f, r, g, b, a * (1.0f - time2), true);
                break;
        }
    }

    private static void drawShape(Matrix4f matrix, VertexConsumer buffer, Waypoint waypoint, double x, double y, double z, float radius, float thickness, double height, float r, float g, float b, float a, boolean drawCaps) {
        if (waypoint.shape == org.blackum.blackaddons.core.waypoint.WaypointShape.BOX) {
            drawBoxShell(matrix, buffer, x, y, z, radius, thickness, height, r, g, b, a, drawCaps);
        } else {
            drawCylindricalShell(matrix, buffer, x, y, z, radius, thickness, height, r, g, b, a, drawCaps);
        }
    }

    private static void drawCylindricalShell(Matrix4f matrix, VertexConsumer buffer, double x, double y, double z, float radius, float thickness, double height, float r, float g, float b, float a, boolean drawCaps) {
        float bottomY = (float) y + 0.01f;
        float topY = bottomY + (float) height;

        for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
            float angle1 = (float) (i * 2 * Math.PI / CIRCLE_SEGMENTS);
            float angle2 = (float) ((i + 1) * 2 * Math.PI / CIRCLE_SEGMENTS);

            float cos1 = (float) Math.cos(angle1);
            float sin1 = (float) Math.sin(angle1);
            float cos2 = (float) Math.cos(angle2);
            float sin2 = (float) Math.sin(angle2);

            float x1_inner = (float) (x + (radius - thickness) * cos1);
            float z1_inner = (float) (z + (radius - thickness) * sin1);
            float x2_inner = (float) (x + (radius - thickness) * cos2);
            float z2_inner = (float) (z + (radius - thickness) * sin2);

            float x1_outer = (float) (x + (radius + thickness) * cos1);
            float z1_outer = (float) (z + (radius + thickness) * sin1);
            float x2_outer = (float) (x + (radius + thickness) * cos2);
            float z2_outer = (float) (z + (radius + thickness) * sin2);

            if (drawCaps) {
                buffer.addVertex(matrix, x1_inner, topY, z1_inner).setColor(r, g, b, a).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(Brightness.FULL_BRIGHT.pack()).setNormal(0, 1, 0);
                buffer.addVertex(matrix, x2_inner, topY, z2_inner).setColor(r, g, b, a).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(Brightness.FULL_BRIGHT.pack()).setNormal(0, 1, 0);
                buffer.addVertex(matrix, x2_outer, topY, z2_outer).setColor(r, g, b, a).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(Brightness.FULL_BRIGHT.pack()).setNormal(0, 1, 0);
                buffer.addVertex(matrix, x1_outer, topY, z1_outer).setColor(r, g, b, a).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(Brightness.FULL_BRIGHT.pack()).setNormal(0, 1, 0);

                buffer.addVertex(matrix, x1_outer, bottomY, z1_outer).setColor(r, g, b, a).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(Brightness.FULL_BRIGHT.pack()).setNormal(0, -1, 0);
                buffer.addVertex(matrix, x2_outer, bottomY, z2_outer).setColor(r, g, b, a).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(Brightness.FULL_BRIGHT.pack()).setNormal(0, -1, 0);
                buffer.addVertex(matrix, x2_inner, bottomY, z2_inner).setColor(r, g, b, a).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(Brightness.FULL_BRIGHT.pack()).setNormal(0, -1, 0);
                buffer.addVertex(matrix, x1_inner, bottomY, z1_inner).setColor(r, g, b, a).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(Brightness.FULL_BRIGHT.pack()).setNormal(0, -1, 0);
            }

            buffer.addVertex(matrix, x1_outer, bottomY, z1_outer).setColor(r, g, b, a).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(Brightness.FULL_BRIGHT.pack()).setNormal(cos1, 0, sin1);
            buffer.addVertex(matrix, x1_outer, topY, z1_outer).setColor(r, g, b, a).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(Brightness.FULL_BRIGHT.pack()).setNormal(cos1, 0, sin1);
            buffer.addVertex(matrix, x2_outer, topY, z2_outer).setColor(r, g, b, a).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(Brightness.FULL_BRIGHT.pack()).setNormal(cos2, 0, sin2);
            buffer.addVertex(matrix, x2_outer, bottomY, z2_outer).setColor(r, g, b, a).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(Brightness.FULL_BRIGHT.pack()).setNormal(cos2, 0, sin2);

            buffer.addVertex(matrix, x1_inner, bottomY, z1_inner).setColor(r, g, b, a).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(Brightness.FULL_BRIGHT.pack()).setNormal(-cos1, 0, -sin1);
            buffer.addVertex(matrix, x2_inner, bottomY, z2_inner).setColor(r, g, b, a).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(Brightness.FULL_BRIGHT.pack()).setNormal(-cos2, 0, -sin2);
            buffer.addVertex(matrix, x2_inner, topY, z2_inner).setColor(r, g, b, a).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(Brightness.FULL_BRIGHT.pack()).setNormal(-cos2, 0, -sin2);
            buffer.addVertex(matrix, x1_inner, topY, z1_inner).setColor(r, g, b, a).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(Brightness.FULL_BRIGHT.pack()).setNormal(-cos1, 0, -sin1);
        }
    }

    private static void drawBoxShell(Matrix4f matrix, VertexConsumer buffer, double x, double y, double z, float radius, float thickness, double height, float r, float g, float b, float a, boolean drawCaps) {
        float bY = (float) y + 0.01f;
        float tY = bY + (float) height;

        float offI = radius - thickness;
        float offO = radius + thickness;

        float x1o = (float)x - offO, z1o = (float)z - offO;
        float x2o = (float)x + offO, z2o = (float)z - offO;
        float x3o = (float)x + offO, z3o = (float)z + offO;
        float x4o = (float)x - offO, z4o = (float)z + offO;

        float x1i = (float)x - offI, z1i = (float)z - offI;
        float x2i = (float)x + offI, z2i = (float)z - offI;
        float x3i = (float)x + offI, z3i = (float)z + offI;
        float x4i = (float)x - offI, z4i = (float)z + offI;

        if (drawCaps) {
            addQuad(matrix, buffer, x1i, tY, z1i, x2i, tY, z2i, x2o, tY, z2o, x1o, tY, z1o, r, g, b, a, 0, 1, 0);
            addQuad(matrix, buffer, x2i, tY, z2i, x3i, tY, z3i, x3o, tY, z3o, x2o, tY, z2o, r, g, b, a, 0, 1, 0);
            addQuad(matrix, buffer, x3i, tY, z3i, x4i, tY, z4i, x4o, tY, z4o, x3o, tY, z3o, r, g, b, a, 0, 1, 0);
            addQuad(matrix, buffer, x4i, tY, z4i, x1i, tY, z1i, x1o, tY, z1o, x4o, tY, z4o, r, g, b, a, 0, 1, 0);

            addQuad(matrix, buffer, x1o, bY, z1o, x2o, bY, z2o, x2i, bY, z2i, x1i, bY, z1i, r, g, b, a, 0, -1, 0);
            addQuad(matrix, buffer, x2o, bY, z2o, x3o, bY, z3o, x3i, bY, z3i, x2i, bY, z2i, r, g, b, a, 0, -1, 0);
            addQuad(matrix, buffer, x3o, bY, z3o, x4o, bY, z4o, x4i, bY, z4i, x3i, bY, z3i, r, g, b, a, 0, -1, 0);
            addQuad(matrix, buffer, x4o, bY, z4o, x1o, bY, z1o, x1i, bY, z1i, x4i, bY, z4i, r, g, b, a, 0, -1, 0);
        }

        addQuad(matrix, buffer, x1o, bY, z1o, x2o, bY, z2o, x2o, tY, z2o, x1o, tY, z1o, r, g, b, a, 0, 0, -1);
        addQuad(matrix, buffer, x2o, bY, z2o, x3o, bY, z3o, x3o, tY, z3o, x2o, tY, z2o, r, g, b, a, 1, 0, 0);
        addQuad(matrix, buffer, x3o, bY, z3o, x4o, bY, z4o, x4o, tY, z4o, x3o, tY, z3o, r, g, b, a, 0, 0, 1);
        addQuad(matrix, buffer, x4o, bY, z4o, x1o, bY, z1o, x1o, tY, z1o, x4o, tY, z4o, r, g, b, a, -1, 0, 0);

        addQuad(matrix, buffer, x1i, bY, z1i, x1i, tY, z1i, x2i, tY, z2i, x2i, bY, z2i, r, g, b, a, 0, 0, 1);
        addQuad(matrix, buffer, x2i, bY, z2i, x2i, tY, z2i, x3i, tY, z3i, x3i, bY, z3i, r, g, b, a, -1, 0, 0);
        addQuad(matrix, buffer, x3i, bY, z3i, x3i, tY, z3i, x4i, tY, z4i, x4i, bY, z4i, r, g, b, a, 0, 0, -1);
        addQuad(matrix, buffer, x4i, bY, z4i, x4i, tY, z4i, x1i, tY, z1i, x1i, bY, z1i, r, g, b, a, 1, 0, 0);
    }

    private static void addQuad(Matrix4f matrix, VertexConsumer buffer, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float r, float g, float b, float a, float nx, float ny, float nz) {
        buffer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(Brightness.FULL_BRIGHT.pack()).setNormal(nx, ny, nz);
        buffer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(Brightness.FULL_BRIGHT.pack()).setNormal(nx, ny, nz);
        buffer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(Brightness.FULL_BRIGHT.pack()).setNormal(nx, ny, nz);
        buffer.addVertex(matrix, x4, y4, z4).setColor(r, g, b, a).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(Brightness.FULL_BRIGHT.pack()).setNormal(nx, ny, nz);
    }
}
