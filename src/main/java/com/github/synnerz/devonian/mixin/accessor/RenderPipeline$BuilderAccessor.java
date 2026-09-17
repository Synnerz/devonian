package com.github.synnerz.devonian.mixin.accessor;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.ShaderType;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(RenderPipeline.Builder.class)
public interface RenderPipeline$BuilderAccessor {
    @Accessor(value = "shaders", remap = false)
    Map<ShaderType, Identifier> getShaders();
}
