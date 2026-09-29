package com.github.synnerz.devonian.mixin.accessor;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.backend.opengl.GlDevice;
import com.mojang.renderpearl.backend.opengl.GlRenderPipeline;
import com.mojang.renderpearl.backend.opengl.GlShaderModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(GlDevice.class)
public interface GlDeviceAccessor {
//    @Accessor("pipelineCache")
//    Map<RenderPipeline, GlRenderPipeline> dv_getPipelineCache();
//
//    @Accessor("shaderCache")
//    Map<GlDevice.ShaderCompilationKey, GlShaderModule> dv_getShaderCache();
}
