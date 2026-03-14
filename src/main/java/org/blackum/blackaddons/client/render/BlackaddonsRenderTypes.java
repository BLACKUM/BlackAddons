package org.blackum.blackaddons.client.render;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class BlackaddonsRenderTypes {
    public static RenderType getWaypoint() {
        Identifier whiteTexture = Identifier.tryParse("minecraft:textures/block/white_concrete.png");
        return RenderTypes.entityTranslucent(whiteTexture);
    }
}
