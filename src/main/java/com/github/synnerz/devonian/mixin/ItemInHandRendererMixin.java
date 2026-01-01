package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.ItemAnimations;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionfc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
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
        if (ItemAnimations.INSTANCE.disableReequip() || ItemAnimations.INSTANCE.disableSwingBob()) return 1f;
        return original.call(instance, v);
    }

    @WrapWithCondition(
        method = "renderHandsWithItems",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionfc;)V")
    )
    private boolean devonian$itemAnimationsSway(PoseStack instance, Quaternionfc quaternionfc) {
        return !ItemAnimations.INSTANCE.disableHandSway();
    }

    @Inject(
        method = "renderPlayerArm",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;getPlayerRenderer(Lnet/minecraft/client/player/AbstractClientPlayer;)Lnet/minecraft/client/renderer/entity/player/AvatarRenderer;")
    )
    private void devonian$itemAnimations1(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int i, float f, float g, HumanoidArm humanoidArm, CallbackInfo ci) {
        ItemAnimations.INSTANCE.applyScale(poseStack);
    }

    @Inject(
        method = "renderItem",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V")
    )
    private void devonian$itemAnimations2(LivingEntity livingEntity, ItemStack itemStack, ItemDisplayContext itemDisplayContext, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int i, CallbackInfo ci) {
        ItemAnimations.INSTANCE.applyScale(poseStack);
    }

    @Inject(
        method = "renderMapHand",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/core/ClientAsset$Texture;texturePath()Lnet/minecraft/resources/ResourceLocation;")
    )
    private void devonian$itemAnimations3(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int i, HumanoidArm humanoidArm, CallbackInfo ci) {
        ItemAnimations.INSTANCE.applyScale(poseStack);
    }

    @Inject(
        method = "renderMap",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitCustomGeometry(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/RenderType;Lnet/minecraft/client/renderer/SubmitNodeCollector$CustomGeometryRenderer;)V")
    )
    private void devonian$itemAnimations4(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int i, ItemStack itemStack, CallbackInfo ci) {
        if (!ItemAnimations.INSTANCE.isEnabled()) return;
        poseStack.translate(64f, 64f, 0f);
        ItemAnimations.INSTANCE.applyScale(poseStack);
        poseStack.translate(-64f, -64f, 0f);
    }

    @ModifyVariable(
        method = "renderPlayerArm",
        at = @At(value = "STORE"),
        ordinal = 4
    )
    private float devonian$itemAnimations5(float f) {
        return ItemAnimations.INSTANCE.disableSwingTranslation() ? 0f : f;
    }

    @ModifyVariable(
        method = "renderPlayerArm",
        at = @At(value = "STORE"),
        ordinal = 5
    )
    private float devonian$itemAnimations6(float f) {
        return ItemAnimations.INSTANCE.disableSwingTranslation() ? 0f : f;
    }

    @ModifyVariable(
        method = "renderPlayerArm",
        at = @At(value = "STORE"),
        ordinal = 6
    )
    private float devonian$itemAnimations7(float f) {
        return ItemAnimations.INSTANCE.disableSwingTranslation() ? 0f : f;
    }

    @WrapOperation(
        method = "renderTwoHandedMap",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V", ordinal = 0)
    )
    private void devonian$itemAnimations8(PoseStack instance, float f, float g, float h, Operation<Void> original) {
        if (ItemAnimations.INSTANCE.disableSwingTranslation()) return;
        original.call(instance, f, g, h);
    }

    @WrapOperation(
        method = "renderOneHandedMap",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V", ordinal = 2)
    )
    private void devonian$itemAnimations9(PoseStack instance, float f, float g, float h, Operation<Void> original) {
        if (ItemAnimations.INSTANCE.disableSwingTranslation()) return;
        original.call(instance, f, g, h);
    }

    @WrapOperation(
        method = "swingArm",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V")
    )
    private void devonian$itemAnimations10(PoseStack instance, float f, float g, float h, Operation<Void> original) {
        if (ItemAnimations.INSTANCE.disableSwingTranslation()) return;
        original.call(instance, f, g, h);
    }
}
