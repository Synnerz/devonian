package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.DisableSwim;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public class PlayerMixin {
    @WrapOperation(
        method = "updatePlayerPose",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setPose(Lnet/minecraft/world/entity/Pose;)V")
    )
    private void devonian$disableSwim(Player instance, Pose pose, Operation<Void> original) {
        if (
            DisableSwim.INSTANCE.isEnabled() &&
            pose == Pose.SWIMMING &&
            instance instanceof LocalPlayer
        ) return;
        original.call(instance, pose);
    }
}
