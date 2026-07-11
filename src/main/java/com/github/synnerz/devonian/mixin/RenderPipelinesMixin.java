package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.mixin.accessor.RenderPipeline$BuilderAccessor;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RenderPipelines.class)
public class RenderPipelinesMixin {
    @WrapOperation(
        method = "<clinit>",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderPipeline$Builder;build()Lcom/mojang/blaze3d/pipeline/RenderPipeline;"),
        remap = false
    )
    private static RenderPipeline devonian$chromaText(RenderPipeline.Builder instance, Operation<RenderPipeline> original) {
        RenderPipeline$BuilderAccessor rp = (RenderPipeline$BuilderAccessor) instance;

        if (
            rp.getFragmentShader().isPresent() && rp.getFragmentShader().get().toString().equals("minecraft:core/text") ||
            rp.getVertexShader().isPresent() && rp.getVertexShader().get().toString().equals("minecraft:core/text")
        ) {
            instance
                .withBindGroupLayout(BindGroupLayout
                        .builder()
                        .withUniform("DevonianChromaInfo", UniformType.UNIFORM_BUFFER)
                        .build())
                .withShaderDefine("DEVONIAN_CHROMA_TEXT");
        }

        return original.call(instance);
    }
}
