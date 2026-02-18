package com.github.synnerz.devonian.mixin.accessor;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Optional;

@Mixin(RenderPipeline.Builder.class)
public interface RenderPipeline$BuilderAccessor {
    @Accessor(value = "fragmentShader", remap = false)
    Optional<Identifier> getFragmentShader();

    @Accessor(value = "vertexShader", remap = false)
    Optional<Identifier> getVertexShader();
}
