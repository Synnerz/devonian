package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.api.events.BlockInteractEvent;
import com.github.synnerz.devonian.features.misc.FixRedVignette;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.border.WorldBorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

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

    @WrapOperation(method = "performUseItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;isFeatureEnabled(Lnet/minecraft/world/flag/FeatureFlagSet;)Z"))
    private boolean devonian$onItemUse(ClientPacketListener instance, FeatureFlagSet featureFlagSet, Operation<Boolean> original, @Local BlockPos blockPos, @Local ItemStack itemStack) {
        if (new BlockInteractEvent(itemStack, blockPos).post()) return false;
        return original.call(instance, featureFlagSet);
    }
}
