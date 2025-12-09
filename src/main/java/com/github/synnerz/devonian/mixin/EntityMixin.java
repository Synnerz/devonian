package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.ChangeCrouchHeight;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(
        method = "getEyeHeight()F",
        at = @At("HEAD"),
        cancellable = true
    )
    private void devonian$sneakHeight(CallbackInfoReturnable<Float> cir) {
        if (!ChangeCrouchHeight.INSTANCE.isEnabled()) return;
        if (!ChangeCrouchHeight.INSTANCE.changeNonVisual()) return;
        Entity that = (Entity) (Object) this;
        if (!(that instanceof LocalPlayer)) return;
        cir.setReturnValue(ChangeCrouchHeight.INSTANCE.getEyeHeight());
    }

    @Inject(
        method = "getEyeHeight(Lnet/minecraft/world/entity/Pose;)F",
        at = @At("HEAD"),
        cancellable = true
    )
    private void devonian$sneakHeight(Pose pose, CallbackInfoReturnable<Float> cir) {
        if (!ChangeCrouchHeight.INSTANCE.isEnabled()) return;
        if (!ChangeCrouchHeight.INSTANCE.changeNonVisual()) return;
        Entity that = (Entity) (Object) this;
        if (!(that instanceof LocalPlayer)) return;
        cir.setReturnValue(ChangeCrouchHeight.INSTANCE.getEyeHeight(pose));
    }
}
