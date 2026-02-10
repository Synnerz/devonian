package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.utils.render.ChromaText;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderSystem.class)
public class RenderSystemMixin {
    @Inject(
        method = "bindDefaultUniforms",
        at = @At("TAIL"),
        remap = false
    )
    private static void devonian$chromaText(RenderPass renderPass, CallbackInfo ci) {
        ChromaText.INSTANCE.bindUniforms(renderPass);
    }
}
