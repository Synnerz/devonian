package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.mixin.accessor.RenderPipeline$BuilderAccessor;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.ShaderType;
import com.mojang.renderpearl.api.pipeline.UniformType;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RenderPipelines.class)
public class RenderPipelinesMixin {
    @Unique
    private static final BindGroupLayout layout = BindGroupLayout
            .builder()
            .withUniform("Global", UniformType.UNIFORM_BUFFER)
            .withUniform("DevonianChromaInfo", UniformType.UNIFORM_BUFFER)
            .build();

    @WrapOperation(
        method = "<clinit>",
        at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/pipeline/RenderPipeline$Builder;build()Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;"),
        remap = false
    )
    private static RenderPipeline devonian$chromaText(RenderPipeline.Builder instance, Operation<RenderPipeline> original) {
        RenderPipeline$BuilderAccessor rp = (RenderPipeline$BuilderAccessor) instance;
        var fragShader = rp.getShaders().get(ShaderType.FRAGMENT);
        var verxShader = rp.getShaders().get(ShaderType.VERTEX);

        if (
            verxShader != null && verxShader.toString().equals("minecraft:core/rendertype_text") ||
            fragShader != null && fragShader.toString().equals("minecraft:core/rendertype_text")
        ) {
            instance
                .withBindGroupLayout(layout)
                .withShaderDefine("DEVONIAN_CHROMA_TEXT");
        }

        return original.call(instance);
    }
}
