package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.RemoveGlowEffect;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    private void devonian$isEntityGlowing(CallbackInfoReturnable<Boolean> cir) {
        if (RemoveGlowEffect.INSTANCE.isEnabled())
            cir.setReturnValue(false);
    }
}
