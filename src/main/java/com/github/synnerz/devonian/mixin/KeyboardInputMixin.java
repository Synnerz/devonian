package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.AutoSprint;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(KeyboardInput.class)
public class KeyboardInputMixin {
    @WrapOperation(method = "tick", at = @At(value = "NEW", target = "(ZZZZZZZ)Lnet/minecraft/world/entity/player/Input;"))
    private Input devonian$onInputTick(boolean up, boolean down, boolean left, boolean right, boolean jump, boolean shift, boolean sprint, Operation<Input> original) {
        if (!AutoSprint.INSTANCE.isEnabled()) return original.call(up, down, left, right, jump, shift, sprint);
        return original.call(up, down, left, right, jump, shift, true);
    }
}
