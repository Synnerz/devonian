package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.PlayerScale;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AvatarRenderer.class)
public class AvatarRendererMixin {
    @WrapOperation(
            method = "scale*",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V"
            )
    )
    private void devonian$onAvatarScale(PoseStack instance, float f, float g, float h, Operation<Void> original) {
        float scale = PlayerScale.INSTANCE.scale();
        if (scale == -1f) {
            original.call(instance, f, g, h);
            return;
        }

        original.call(instance, scale, scale, scale);
    }
}
