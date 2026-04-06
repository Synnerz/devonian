package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.utils.render.Render3DPipelines;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.opengl.GlCommandEncoder;
import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GlCommandEncoder.class)
public class GlCommandEncoderMixin {
    @Shadow
    @Nullable
    private RenderPipeline lastPipeline;

    // FIXME
//    @WrapOperation(
//        method = "applyPipelineState",
//        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/opengl/GlConst;toGl(Lcom/mojang/blaze3d/platform/DepthTestFunction;)I", remap = false)
//    )
//    private int getDepthFunc(DepthTestFunction depthTestFunction, Operation<Integer> original) {
//        if (!Render3DPipelines.getALWAYS_PASS_RENDER_PIPELINES().contains(lastPipeline)) return original.call(depthTestFunction);
//        return GlConst.GL_ALWAYS;
//    }
}
