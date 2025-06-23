package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.NoHurtCamera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void devonian$onHurtCam(MatrixStack matrices, float tickProgress, CallbackInfo ci) {
        if (!NoHurtCamera.INSTANCE.isEnabled()) return;
        ci.cancel();
    }
}
