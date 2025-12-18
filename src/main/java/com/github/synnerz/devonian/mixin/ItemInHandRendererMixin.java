package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.ItemAnimations;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {
    @WrapOperation(
        method = "renderHandsWithItems",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getAttackAnim(F)F")
    )
    private float devonian$itemAnimationsSwing(LocalPlayer instance, float v, Operation<Float> original) {
        if (!ItemAnimations.INSTANCE.isEnabled()) return original.call(instance, v);
        return ItemAnimations.INSTANCE.getSwingAnimation(v);
    }

    @Inject(
        method = "shouldInstantlyReplaceVisibleItem",
        at = @At("HEAD"),
        cancellable = true
    )
    private void devonian$itemAnimationsReequip(ItemStack itemStack, ItemStack itemStack2, CallbackInfoReturnable<Boolean> cir) {
        if (ItemAnimations.INSTANCE.disableReequip()) cir.setReturnValue(true);
    }

    @Inject(
        method = "renderHandsWithItems",
        at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
                ordinal = 0
        )
    )
    private void devonian$itemAnimations(float f, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, LocalPlayer localPlayer, int i, CallbackInfo ci) {
        ItemAnimations.INSTANCE.applyTransformations(poseStack);
    }

    @WrapOperation(
        method = "tick",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getAttackStrengthScale(F)F")
    )
    private float devonian$itemAnimationsBob(LocalPlayer instance, float v, Operation<Float> original) {
        if (ItemAnimations.INSTANCE.disableReequip() || ItemAnimations.INSTANCE.disableSwing()) return 1f;
        return original.call(instance, v);
    }
}
