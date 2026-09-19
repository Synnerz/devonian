package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.Fullbright;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.api.pipeline.ShaderType;
import com.mojang.renderpearl.backend.opengl.GlPipelineRecompiler;
import com.mojang.renderpearl.backend.opengl.GlShaderModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

import java.util.Objects;

@Mixin(GlPipelineRecompiler.class)
public class GlPipelineRecompilerMixin {
    @WrapOperation(
        method = "compileProgram",
        at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/backend/opengl/GlPipelineRecompiler;compileShader(Ljava/lang/String;Lcom/mojang/renderpearl/api/pipeline/ShaderType;Ljava/lang/String;)Lcom/mojang/renderpearl/backend/opengl/GlShaderModule;")
    )
    private GlShaderModule devonian$fullbright(
            GlPipelineRecompiler instance, String name, ShaderType type, String source, Operation<GlShaderModule> original
    ) {
        if (!Fullbright.INSTANCE.isEnabled()) return original.call(instance, name, type, source);

        if (type != ShaderType.FRAGMENT || !Objects.equals(name, "minecraft:core/lightmap"))
            return original.call(instance, name, type, source);

        return original.call(instance, name, type, """
            #version 150
            
            in vec2 texCoord;
            out vec4 fragColor;
            
            void main() {
                fragColor = vec4(1.0);
            }
            """);
    }
}
