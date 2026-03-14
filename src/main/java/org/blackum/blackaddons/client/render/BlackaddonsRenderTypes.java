package org.blackum.blackaddons.client.render;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class BlackaddonsRenderTypes {
    public static RenderType getWaypoint() {
        ResourceLocation whiteTexture = ResourceLocation.parse("minecraft:textures/block/white_concrete.png");
        return RenderType.entityTranslucent(whiteTexture);
    }
}
