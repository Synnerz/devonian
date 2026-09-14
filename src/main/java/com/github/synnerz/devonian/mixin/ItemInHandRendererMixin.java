package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.ItemAnimations;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionfc;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FirstPersonHandsAndItemsRenderer.class)
public class ItemInHandRendererMixin {
//    @Shadow
//    private ItemStack mainHandItem;

    @Shadow
    @Final
    private Minecraft minecraft;

    @ModifyExpressionValue(
            method = "submitHandsWithItems",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;swingAnimation:F",
                    opcode = Opcodes.GETFIELD
            )
    )
    private float devonian$itemAnimationsSwing(float original, @Local(argsOnly = true, name = "partialTicks") float partialTicks) {
        if (!ItemAnimations.INSTANCE.isEnabled()) return original;
        return ItemAnimations.INSTANCE.getSwingAnimation(partialTicks);
    }

//    @Inject(
//        method = "shouldInstantlyReplaceVisibleItem",
//        at = @At("HEAD"),
//        cancellable = true
//    )
//    private void devonian$itemAnimationsReequip(ItemStack itemStack, ItemStack itemStack2, CallbackInfoReturnable<Boolean> cir) {
//        if (ItemAnimations.INSTANCE.disableReequip()) cir.setReturnValue(true);
//    }

    @Inject(
        method = "submitHandsWithItems",
        at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/client/renderer/FirstPersonHandsAndItemsRenderer;submitArmWithItem(Lnet/minecraft/client/renderer/state/level/PlayerRenderState;Lnet/minecraft/client/renderer/state/level/FirstPersonHandsAndItemsRenderState;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
                ordinal = 0
        )
    )
    private void devonian$itemAnimations(float partialTicks, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, PlayerRenderState playerState, FirstPersonHandsAndItemsRenderState state, CallbackInfo ci) {
        if (minecraft.player == null) return;
        ItemStack mainHandItem = minecraft.player.getMainHandItem();
        if (mainHandItem.isEmpty() && !ItemAnimations.INSTANCE.affectHand()) return;
        if (mainHandItem.has(DataComponents.MAP_ID) && !ItemAnimations.INSTANCE.affectMap()) return;
        ItemAnimations.INSTANCE.applyTransformations(poseStack);
    }

//    @WrapOperation(
//        method = "tick",
//        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getItemSwapScale(F)F")
//    )
//    private float devonian$itemAnimationsBob(LocalPlayer instance, float v, Operation<Float> original) {
//        if (ItemAnimations.INSTANCE.disableReequip() || ItemAnimations.INSTANCE.disableSwingBob()) return 1f;
//        return original.call(instance, v);
//    }

    @WrapWithCondition(
        method = "submitHandsWithItems",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;rotateDegrees(Lcom/mojang/math/Axis;F)V")
    )
    private boolean devonian$itemAnimationsSway(PoseStack instance, Axis axis, float angle) {
        return !ItemAnimations.INSTANCE.disableHandSway();
    }

    @Inject(
        method = "renderPlayerHand",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;getRenderer(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)Lnet/minecraft/client/renderer/entity/player/AvatarRenderer;")
    )
    private void devonian$itemAnimations1(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, HumanoidArm arm, PlayerRenderState playerState, CallbackInfo ci) {
        if (!ItemAnimations.INSTANCE.affectHand()) return;
        ItemAnimations.INSTANCE.applyScale(poseStack);
    }

//    @Inject(
//        method = "renderItem",
//        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V")
//    )
//    private void devonian$itemAnimations2(LivingEntity livingEntity, ItemStack itemStack, ItemDisplayContext itemDisplayContext, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int i, CallbackInfo ci) {
//        ItemAnimations.INSTANCE.applyScale(poseStack);
//    }

    @Inject(
        method = "renderPlayerHand",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/core/ClientAsset$Texture;texturePath()Lnet/minecraft/resources/Identifier;")
    )
    private void devonian$itemAnimations3(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, HumanoidArm arm, PlayerRenderState playerState, CallbackInfo ci) {
        if (!ItemAnimations.INSTANCE.affectMap()) return;
        ItemAnimations.INSTANCE.applyScale(poseStack);
    }

   @Inject(
       method = "renderMap",
       at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitCustomGeometry(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/SubmitNodeCollector$CustomGeometryRenderer;)V")
   )
   private void devonian$itemAnimations4(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, ItemStack itemStack, boolean mainHand, FirstPersonHandsAndItemsRenderState state, CallbackInfo ci) {
       if (!ItemAnimations.INSTANCE.isEnabled()) return;
       if (!ItemAnimations.INSTANCE.affectMap()) return;
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
