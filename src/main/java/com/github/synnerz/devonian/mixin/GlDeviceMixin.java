package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.Fullbright;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.shaders.ShaderType;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.BiFunction;

@Mixin(GlDevice.class)
public class GlDeviceMixin {

    @WrapOperation(
        method = "compileShader",
        at = @At(value = "INVOKE", target = "Ljava/util/function/BiFunction;apply(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    )
    private Object devonian$fullbright(
        BiFunction<ResourceLocation, ShaderType, String> instance,
        Object id_,
        Object type_,
        Operation<String> original
    ) {
        if (!Fullbright.INSTANCE.isEnabled()) return original.call(instance, id_, type_);

        ResourceLocation id = (ResourceLocation) id_;
        ShaderType type = (ShaderType) type_;
        if (type != ShaderType.FRAGMENT || !id.equals(RenderPipelines.LIGHTMAP.getFragmentShader()))
            return original.call(instance, id_, type_);

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
