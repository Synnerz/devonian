package com.github.synnerz.devonian.mixin.accessor;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.StagedVertexBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(StagedVertexBuffer.Draw.class)
public interface StagedVertexBufferDrawAccessor {
    @Accessor
    VertexFormat getFormat();
    @Accessor
    PrimitiveTopology getPrimitiveTopology();
}
