package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.ItemAnimations;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public class PlayerMixin {
    @WrapOperation(
        method = "aiStep",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;updateSwingTime()V")
    )
    private void devonian$itemAnimations(Player instance, Operation<Void> original) {
        if (!ItemAnimations.INSTANCE.isEnabled()) {
            original.call(instance);
            return;
        }

        ItemAnimations.INSTANCE.onUpdateSwingTime();
    }
}
