package com.github.synnerz.devonian.mixin.accessor;

import com.mojang.blaze3d.pipeline.PipelineCache;
import com.mojang.blaze3d.systems.RenderSystem;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderSystem.class)
public interface RenderSystemAccessor {
    @Accessor("currentPipelineCache")
    @Nullable
    static PipelineCache getCurrentPipelineCache() {
        throw new AssertionError();
    }

    @Accessor("fallbackPipelineCache")
    @Nullable
    static PipelineCache getFallbackPipelineCache() {
        throw new AssertionError();
    }
}
