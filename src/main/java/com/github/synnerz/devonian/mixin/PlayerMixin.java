package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.ItemAnimations;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public class PlayerMixin {
    @Inject(
        method = "aiStep",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;updateSwingTime()V")
    )
    private void devonian$itemAnimations(CallbackInfo ci) {
        ItemAnimations.INSTANCE.onUpdateSwingTime();
    }
}
