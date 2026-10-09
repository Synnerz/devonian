package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.ItemAnimations;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.player.FirstPersonHandsAndItems;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FirstPersonHandsAndItems.class)
public class FirstPersonHandsAndItemsMixin {
    @Inject(
            method = "shouldInstantlyReplaceVisibleItem",
            at = @At("HEAD"),
            cancellable = true
    )
    private void devonian$itemAnimationsReequip(ItemStack currentlyVisibleItem, ItemStack expectedItem, LocalPlayer player, CallbackInfoReturnable<Boolean> cir) {
        if (ItemAnimations.INSTANCE.disableReequip()) cir.setReturnValue(true);
    }

    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getItemSwapScale(F)F")
    )
    private float devonian$itemAnimationsBob(LocalPlayer instance, float v, Operation<Float> original) {
        if (ItemAnimations.INSTANCE.disableReequip() || ItemAnimations.INSTANCE.disableSwingBob()) return 1f;
        return original.call(instance, v);
    }
}
