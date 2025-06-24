package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.RemoveLightning;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LightningEntityRenderer;
import net.minecraft.client.render.entity.state.LightningEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightningEntityRenderer.class)
public class LightningEntityRendererMixin {
    @Inject(
            method = "render(Lnet/minecraft/client/render/entity/state/LightningEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void devonian$onLightning(LightningEntityRenderState lightningEntityRenderState, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        if (!RemoveLightning.INSTANCE.isEnabled()) return;
        ci.cancel();
    }
}
