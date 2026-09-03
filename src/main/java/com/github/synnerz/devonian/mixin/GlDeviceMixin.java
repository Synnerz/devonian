package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.Fullbright;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.api.pipeline.ShaderSource;
import com.mojang.renderpearl.api.pipeline.ShaderType;
import com.mojang.renderpearl.backend.opengl.GlDevice;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GlDevice.class)
public class GlDeviceMixin {
    @WrapOperation(
        method = "compileShader",
        at = @At(
                value = "INVOKE",
                target = "Lcom/mojang/blaze3d/shaders/ShaderSource;get(Lnet/minecraft/resources/Identifier;Lcom/mojang/blaze3d/shaders/ShaderType;)Ljava/lang/String;"
        )
    )
    private String devonian$fullbright(
            ShaderSource instance, Identifier identifier, ShaderType shaderType, Operation<String> original
    ) {
        if (!Fullbright.INSTANCE.isEnabled()) return original.call(instance, identifier, shaderType);

        if (shaderType != ShaderType.FRAGMENT || !identifier.equals(RenderPipelines.LIGHTMAP.getFragmentShader()))
            return original.call(instance, identifier, shaderType);

        return """
            #version 150
            
            in vec2 texCoord;
            out vec4 fragColor;
            
            void main() {
                fragColor = vec4(1.0);
            }
            """;
    }
}
