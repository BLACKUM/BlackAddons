package org.blackum.blackaddons.mixin.render;

import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Matrix4fStack;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.blackum.blackaddons.gui.render.DebugBoxRenderer;
import org.blackum.blackaddons.gui.render.WaypointRenderer;
import org.blackum.blackaddons.core.util.LocationUtils;
import org.blackum.blackaddons.feature.cheat.FastLeap;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void onRenderLevel(GraphicsResourceAllocator graphicsResourceAllocator, DeltaTracker deltaTracker, boolean z1, CameraRenderState cameraRenderState, Matrix4fc matrix4fc, GpuBufferSlice gpuBufferSlice, Vector4f vector4f, boolean z2, ChunkSectionsToRender chunkSectionsToRender, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameRenderer == null) return;

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        Matrix4f identity = new Matrix4f();

        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        modelViewStack.mul(matrix4fc);

        WaypointRenderer.render(identity, bufferSource, deltaTracker.getGameTimeDeltaPartialTick(false));

        Vec3 camPos = mc.gameRenderer.getMainCamera().position();
        DebugBoxRenderer.render(identity, bufferSource, camPos, LocationUtils.getDebugBoxes());
        DebugBoxRenderer.render(identity, bufferSource, camPos, FastLeap.getDebugBoxes());

        bufferSource.endBatch();

        modelViewStack.popMatrix();
    }
}
