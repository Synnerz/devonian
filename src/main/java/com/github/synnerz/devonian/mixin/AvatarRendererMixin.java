package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.Devonian;
import com.github.synnerz.devonian.features.misc.OwnNameTag;
import com.github.synnerz.devonian.features.misc.PlayerScale;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin<AvatarlikeEntity extends Avatar & ClientAvatarEntity> extends LivingEntityRenderer<AvatarlikeEntity, AvatarRenderState, PlayerModel> {
    public AvatarRendererMixin(EntityRendererProvider.Context context, PlayerModel entityModel, float f) {
        super(context, entityModel, f);
    }

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

    @WrapMethod(method = "shouldShowName(Lnet/minecraft/world/entity/Avatar;D)Z")
    private boolean devonian$shouldShowTag(AvatarlikeEntity avatar, double d, Operation<Boolean> original) {
        var player = Devonian.INSTANCE.getMinecraft().player;
        if (
                !OwnNameTag.INSTANCE.isEnabled() ||
                player == null ||
                avatar != player ||
                Devonian.INSTANCE.getMinecraft().options.getCameraType().isFirstPerson()
        ) return original.call(avatar, d);

        return true;
    }
}
