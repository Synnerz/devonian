package com.github.synnerz.devonian.mixin.accessor;

import com.mojang.blaze3d.buffers.GpuBuffer;
import net.minecraft.client.renderer.StagedVertexBuffer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(StagedVertexBuffer.class)
public interface StagedVertexBufferAccessor {
    @Nullable
    @Accessor
    StagedVertexBuffer.Draw getLastBuildingDraw();

    @Nullable
    @Accessor
    GpuBuffer getCurrentVertexBuffer();
}
