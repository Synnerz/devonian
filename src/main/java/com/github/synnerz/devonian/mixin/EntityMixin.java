package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.ChangeCrouchHeight;
import com.github.synnerz.devonian.features.misc.DisableSwim;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
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

    @ModifyVariable(
        method = "setSwimming",
        at = @At("HEAD"),
        ordinal = 0,
        argsOnly = true
    )
    private boolean devonian$disableSwim(boolean value) {
        if (!DisableSwim.INSTANCE.isEnabled()) return value;
        Entity that = (Entity) (Object) this;
        if (!(that instanceof LocalPlayer)) return value;
        return false;
    }
}
