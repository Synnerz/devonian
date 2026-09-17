package com.github.synnerz.devonian.mixin.accessor;

import net.minecraft.client.renderer.StagedVertexBuffer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(StagedVertexBuffer.Draw.class)
public interface StagedVertexBufferDrawAccessor {
//    @Accessor
//    VertexFormat getFormat();
//    @Accessor
//    PrimitiveTopology getPrimitiveTopology();
}
