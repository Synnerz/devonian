package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.NoHurtCamera;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void devonian$onHurtCam(PoseStack poseStack, float f, CallbackInfo ci) {
        if (!NoHurtCamera.INSTANCE.isEnabled()) return;
        ci.cancel();
    }
}
