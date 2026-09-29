package com.github.synnerz.devonian.mixin.accessor;

import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlRenderPipeline;
import com.mojang.blaze3d.opengl.GlShaderModule;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(GlDevice.class)
public interface GlDeviceAccessor {
    @Accessor("pipelineCache")
    Map<RenderPipeline, GlRenderPipeline> dv_getPipelineCache();

    @Accessor("shaderCache")
    Map<GlDevice.ShaderCompilationKey, GlShaderModule> dv_getShaderCache();
}
