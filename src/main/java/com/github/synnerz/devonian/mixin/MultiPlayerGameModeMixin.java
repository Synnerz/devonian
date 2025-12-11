package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.FixRedVignette;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
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
}
