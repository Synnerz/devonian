package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.Fullbright;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.api.pipeline.ShaderType;
import com.mojang.renderpearl.frontend.shaders.GlslCompiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(GlslCompiler.class)
public class GlslCompilerMixin {
    @ModifyVariable(
        method = "compileToSpv",
        at = @At("HEAD"),
        name = "source",
        argsOnly = true
    )
    private String devonian$fullbright(
        String source,
        @Local(name = "name", argsOnly = true) String name,
        @Local(name = "type", argsOnly = true) ShaderType type
    ) {
        if (!Fullbright.INSTANCE.isEnabled()) return source;
        if (type != ShaderType.FRAGMENT) return source;
        if (!"minecraft:core/lightmap".equals(name)) return source;

        return """
            #version 330
            #extension GL_ARB_separate_shader_objects : require

            layout(std140) uniform LightmapInfo {
                float SkyFactor;
                float BlockFactor;
                float NightVisionFactor;
                float DarknessScale;
                float BossOverlayWorldDarkeningFactor;
                float BrightnessFactor;
                vec3 BlockLightTint;
                vec3 SkyLightColor;
                vec3 AmbientColor;
                vec3 NightVisionColor;
            } lightmapInfo;

            layout(location = 0) in vec2 texCoord;

            layout(location = 0) out vec4 fragColor;
            
            void main() {
                fragColor = vec4(1.0);
            }
            """;
    }
}
