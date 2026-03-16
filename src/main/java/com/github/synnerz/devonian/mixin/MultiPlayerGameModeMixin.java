package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.api.events.BlockInteractEvent;
import com.github.synnerz.devonian.features.misc.FixRedVignette;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
    @WrapOperation(
        method = "useItemOn",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/border/WorldBorder;isWithinBounds(Lnet/minecraft/core/BlockPos;)Z")
    )
    private boolean devonian$fixRedVignette(WorldBorder instance, BlockPos blockPos, Operation<Boolean> original) {
        if (!FixRedVignette.INSTANCE.isEnabled()) return original.call(instance, blockPos);
        return true;
    }

    @Inject(
            method = "performUseItemOn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/ClientLevel;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void devonian$onItemUse(LocalPlayer localPlayer, InteractionHand interactionHand, BlockHitResult blockHitResult, CallbackInfoReturnable<InteractionResult> cir, @Local BlockPos blockPos, @Local ItemStack itemStack) {
        if (new BlockInteractEvent(itemStack, blockPos).post()) cir.setReturnValue(InteractionResult.PASS);
    }

    @Inject(
            method = "performUseItemOn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/context/UseOnContext;<init>(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void devonian$onItemOnUIse(LocalPlayer localPlayer, InteractionHand interactionHand, BlockHitResult blockHitResult, CallbackInfoReturnable<InteractionResult> cir, @Local BlockPos blockPos, @Local ItemStack itemStack) {
        if (new BlockInteractEvent(itemStack, blockPos).post()) cir.setReturnValue(InteractionResult.PASS);
    }
}
