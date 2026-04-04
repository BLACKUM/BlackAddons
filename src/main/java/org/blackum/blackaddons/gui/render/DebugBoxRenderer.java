package org.blackum.blackaddons.gui.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.UiLightmap;
import net.minecraft.util.Brightness;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import org.blackum.blackaddons.client.render.BlackaddonsRenderTypes;
import org.joml.Matrix4f;

import java.util.List;

public final class DebugBoxRenderer {
    private static final float DEFAULT_THICKNESS = 0.05f;

    private DebugBoxRenderer() {
    }

    public static void render(Matrix4f matrix, MultiBufferSource bufferSource, Vec3 cameraPos, List<BoxSpec> boxes) {
        if (boxes == null || boxes.isEmpty()) return;

        VertexConsumer buffer = bufferSource.getBuffer(BlackaddonsRenderTypes.getWaypoint());
        for (BoxSpec box : boxes) {
            double minX = Math.min(box.minX(), box.maxX()) - cameraPos.x;
            double minY = Math.min(box.minY(), box.maxY()) - cameraPos.y;
            double minZ = Math.min(box.minZ(), box.maxZ()) - cameraPos.z;
            double maxX = Math.max(box.minX(), box.maxX()) - cameraPos.x;
            double maxY = Math.max(box.minY(), box.maxY()) - cameraPos.y;
            double maxZ = Math.max(box.minZ(), box.maxZ()) - cameraPos.z;

            float r = ((box.color() >> 16) & 0xFF) / 255f;
            float g = ((box.color() >> 8) & 0xFF) / 255f;
            float b = (box.color() & 0xFF) / 255f;
            drawBoxShell(matrix, buffer, minX, minY, minZ, maxX, maxY, maxZ, DEFAULT_THICKNESS, r, g, b, box.alpha());
        }
    }

    private static void drawBoxShell(Matrix4f matrix, VertexConsumer buffer, double minX, double minY, double minZ,
                                     double maxX, double maxY, double maxZ, float thickness,
                                     float r, float g, float b, float a) {
        float x1o = (float) minX - thickness;
        float y1o = (float) minY - thickness;
        float z1o = (float) minZ - thickness;
        float x2o = (float) maxX + thickness;
        float y2o = (float) maxY + thickness;
        float z2o = (float) maxZ + thickness;

        float x1i = (float) minX + thickness;
        float y1i = (float) minY + thickness;
        float z1i = (float) minZ + thickness;
        float x2i = (float) maxX - thickness;
        float y2i = (float) maxY - thickness;
        float z2i = (float) maxZ - thickness;

        if (x1i > x2i) {
            float cx = ((float) minX + (float) maxX) * 0.5f;
            x1i = cx;
            x2i = cx;
        }
        if (y1i > y2i) {
            float cy = ((float) minY + (float) maxY) * 0.5f;
            y1i = cy;
            y2i = cy;
        }
        if (z1i > z2i) {
            float cz = ((float) minZ + (float) maxZ) * 0.5f;
            z1i = cz;
            z2i = cz;
        }

        addQuad(matrix, buffer, x1o, y1o, z1o, x2o, y1o, z1o, x2o, y2o, z1o, x1o, y2o, z1o, r, g, b, a, 0, 0, -1);
        addQuad(matrix, buffer, x2o, y1o, z1o, x2o, y1o, z2o, x2o, y2o, z2o, x2o, y2o, z1o, r, g, b, a, 1, 0, 0);
        addQuad(matrix, buffer, x2o, y1o, z2o, x1o, y1o, z2o, x1o, y2o, z2o, x2o, y2o, z2o, r, g, b, a, 0, 0, 1);
        addQuad(matrix, buffer, x1o, y1o, z2o, x1o, y1o, z1o, x1o, y2o, z1o, x1o, y2o, z2o, r, g, b, a, -1, 0, 0);

        addQuad(matrix, buffer, x1i, y1i, z1i, x1i, y2i, z1i, x2i, y2i, z1i, x2i, y1i, z1i, r, g, b, a, 0, 0, 1);
        addQuad(matrix, buffer, x2i, y1i, z1i, x2i, y2i, z1i, x2i, y2i, z2i, x2i, y1i, z2i, r, g, b, a, -1, 0, 0);
        addQuad(matrix, buffer, x2i, y1i, z2i, x2i, y2i, z2i, x1i, y2i, z2i, x1i, y1i, z2i, r, g, b, a, 0, 0, -1);
        addQuad(matrix, buffer, x1i, y1i, z2i, x1i, y2i, z2i, x1i, y2i, z1i, x1i, y1i, z1i, r, g, b, a, 1, 0, 0);

        addQuad(matrix, buffer, x1o, y2o, z1o, x2o, y2o, z1o, x2i, y2i, z1i, x1i, y2i, z1i, r, g, b, a, 0, 1, 0);
        addQuad(matrix, buffer, x2o, y2o, z1o, x2o, y2o, z2o, x2i, y2i, z2i, x2i, y2i, z1i, r, g, b, a, 0, 1, 0);
        addQuad(matrix, buffer, x2o, y2o, z2o, x1o, y2o, z2o, x1i, y2i, z2i, x2i, y2i, z2i, r, g, b, a, 0, 1, 0);
        addQuad(matrix, buffer, x1o, y2o, z2o, x1o, y2o, z1o, x1i, y2i, z1i, x1i, y2i, z2i, r, g, b, a, 0, 1, 0);

        addQuad(matrix, buffer, x1o, y1o, z1o, x1i, y1i, z1i, x2i, y1i, z1i, x2o, y1o, z1o, r, g, b, a, 0, -1, 0);
        addQuad(matrix, buffer, x2o, y1o, z1o, x2i, y1i, z1i, x2i, y1i, z2i, x2o, y1o, z2o, r, g, b, a, 0, -1, 0);
        addQuad(matrix, buffer, x2o, y1o, z2o, x2i, y1i, z2i, x1i, y1i, z2i, x1o, y1o, z2o, r, g, b, a, 0, -1, 0);
        addQuad(matrix, buffer, x1o, y1o, z2o, x1i, y1i, z2i, x1i, y1i, z1i, x1o, y1o, z1o, r, g, b, a, 0, -1, 0);
    }

    private static void addQuad(Matrix4f matrix, VertexConsumer buffer,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                float x3, float y3, float z3,
                                float x4, float y4, float z4,
                                float r, float g, float b, float a,
                                float nx, float ny, float nz) {
        buffer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(Brightness.FULL_BRIGHT.pack()).setNormal(nx, ny, nz);
        buffer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(Brightness.FULL_BRIGHT.pack()).setNormal(nx, ny, nz);
        buffer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(Brightness.FULL_BRIGHT.pack()).setNormal(nx, ny, nz);
        buffer.addVertex(matrix, x4, y4, z4).setColor(r, g, b, a).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(Brightness.FULL_BRIGHT.pack()).setNormal(nx, ny, nz);
    }

    public record BoxSpec(String label, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int color, float alpha) {
    }
}
