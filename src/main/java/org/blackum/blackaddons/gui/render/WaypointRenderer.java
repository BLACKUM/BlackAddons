package org.blackum.blackaddons.gui.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.client.render.BlackaddonsRenderTypes;
import org.blackum.blackaddons.core.waypoint.Waypoint;
import org.blackum.blackaddons.core.waypoint.WaypointAnimation;
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
        renderAnimatedWaypoint(matrix, bufferSource, x, y, z, radius, color, waypoint.height, anim);
    }

    private static void renderAnimatedWaypoint(Matrix4f matrix, MultiBufferSource bufferSource, double x, double y, double z, float radius, Color color, double height, WaypointAnimation animation) {
        VertexConsumer buffer = bufferSource.getBuffer(BlackaddonsRenderTypes.getWaypoint());

        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = color.getAlpha() / 255f;

        switch (animation) {
            case RADAR:
                drawCylindricalShell(matrix, buffer, x, y, z, radius, 0.02f, height, r, g, b, a * 0.8f);
                float radarTime = (System.currentTimeMillis() % 1500) / 1500f;
                float waveRadius = radius * radarTime;
                if (waveRadius > 0.05f) {
                    drawCylindricalShell(matrix, buffer, x, y, z, waveRadius, 0.03f, height * 0.5f, r, g, b, a * (1.0f - radarTime));
                }
                break;
            case PULSE:
                float pulse = Mth.sin((System.currentTimeMillis() % 2000) / 2000f * (float) Math.PI * 2) * 0.1f + 0.9f;
                drawCylindricalShell(matrix, buffer, x, y, z, radius * pulse, 0.05f, height, r, g, b, a);
                break;
            case STATIC:
                drawCylindricalShell(matrix, buffer, x, y, z, radius, 0.05f, height, r, g, b, a);
                break;
            case BOUNCE:
                float bounce = Mth.sin((System.currentTimeMillis() % 1000) / 1000f * (float) Math.PI * 2) * 0.2f;
                drawCylindricalShell(matrix, buffer, x, y + bounce, z, radius, 0.05f, height, r, g, b, a);
                break;
            case BREATH:
                float breath = Mth.sin((System.currentTimeMillis() % 3000) / 3000f * (float) Math.PI * 2) * 0.4f + 0.6f;
                drawCylindricalShell(matrix, buffer, x, y, z, radius, 0.05f, height, r, g, b, a * breath);
                break;
            case DOUBLE_RADAR:
                drawCylindricalShell(matrix, buffer, x, y, z, radius, 0.02f, height, r, g, b, a * 0.6f);
                float time1 = (System.currentTimeMillis() % 2000) / 2000f;
                float time2 = ((System.currentTimeMillis() + 1000) % 2000) / 2000f;
                drawCylindricalShell(matrix, buffer, x, y, z, radius * time1, 0.03f, height * 0.4f, r, g, b, a * (1.0f - time1));
                drawCylindricalShell(matrix, buffer, x, y, z, radius * time2, 0.03f, height * 0.4f, r, g, b, a * (1.0f - time2));
                break;
        }
    }

    private static void drawCylindricalShell(Matrix4f matrix, VertexConsumer buffer, double x, double y, double z, float radius, float thickness, double height, float r, float g, float b, float a) {
        float bottomY = (float) y + 0.05f;
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

            buffer.addVertex(matrix, x1_inner, topY, z1_inner).setColor(r, g, b, a).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 1, 0);
            buffer.addVertex(matrix, x2_inner, topY, z2_inner).setColor(r, g, b, a).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 1, 0);
            buffer.addVertex(matrix, x2_outer, topY, z2_outer).setColor(r, g, b, a).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 1, 0);
            buffer.addVertex(matrix, x1_outer, topY, z1_outer).setColor(r, g, b, a).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 1, 0);

            buffer.addVertex(matrix, x1_outer, bottomY, z1_outer).setColor(r, g, b, a).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, -1, 0);
            buffer.addVertex(matrix, x2_outer, bottomY, z2_outer).setColor(r, g, b, a).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, -1, 0);
            buffer.addVertex(matrix, x2_inner, bottomY, z2_inner).setColor(r, g, b, a).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, -1, 0);
            buffer.addVertex(matrix, x1_inner, bottomY, z1_inner).setColor(r, g, b, a).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, -1, 0);

            buffer.addVertex(matrix, x1_outer, bottomY, z1_outer).setColor(r, g, b, a).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(cos1, 0, sin1);
            buffer.addVertex(matrix, x1_outer, topY, z1_outer).setColor(r, g, b, a).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(cos1, 0, sin1);
            buffer.addVertex(matrix, x2_outer, topY, z2_outer).setColor(r, g, b, a).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(cos2, 0, sin2);
            buffer.addVertex(matrix, x2_outer, bottomY, z2_outer).setColor(r, g, b, a).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(cos2, 0, sin2);

            buffer.addVertex(matrix, x1_inner, bottomY, z1_inner).setColor(r, g, b, a).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(-cos1, 0, -sin1);
            buffer.addVertex(matrix, x2_inner, bottomY, z2_inner).setColor(r, g, b, a).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(-cos2, 0, -sin2);
            buffer.addVertex(matrix, x2_inner, topY, z2_inner).setColor(r, g, b, a).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(-cos2, 0, -sin2);
            buffer.addVertex(matrix, x1_inner, topY, z1_inner).setColor(r, g, b, a).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(-cos1, 0, -sin1);
        }
    }
}
