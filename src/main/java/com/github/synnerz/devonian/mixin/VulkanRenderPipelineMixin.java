package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.utils.render.Render3DPipelines;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vulkan.VulkanRenderPipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(VulkanRenderPipeline.class)
public abstract class VulkanRenderPipelineMixin {
    @WrapOperation(
            method = "compile",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/pipeline/DepthStencilState;depthTest()Lcom/mojang/blaze3d/platform/CompareOp;"
            )
    )
    private static CompareOp devonian$onVulkanDepth(DepthStencilState instance, Operation<CompareOp> original, @Local(argsOnly = true, name = "pipeline") RenderPipeline pipeline) {
        if (!Render3DPipelines.getALWAYS_PASS_RENDER_PIPELINES().contains(pipeline)) return original.call(instance);
        return CompareOp.ALWAYS_PASS;
    }
}