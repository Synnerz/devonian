package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.utils.render.Render3DPipelines;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.backend.opengl.GlCommandEncoder;
import com.mojang.renderpearl.backend.opengl.GlConst;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GlCommandEncoder.class)
public class GlCommandEncoderMixin {
    @Shadow
    @Nullable
    private RenderPipeline lastPipeline;

    @WrapOperation(
            method = "applyPipelineState",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/opengl/GlConst;toGl(Lcom/mojang/blaze3d/platform/CompareOp;)I",
                    remap = false
            )
    )
    private int getDepthFunc(CompareOp compareOp, Operation<Integer> original) {
        if (!Render3DPipelines.getALWAYS_PASS_RENDER_PIPELINES().contains(lastPipeline)) return original.call(compareOp);
        return GlConst.GL_ALWAYS;
    }
}
