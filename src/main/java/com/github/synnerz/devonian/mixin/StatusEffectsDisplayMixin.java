package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.HideInventoryEffects;
import net.minecraft.client.gui.screen.ingame.StatusEffectsDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StatusEffectsDisplay.class)
public class StatusEffectsDisplayMixin {
    @Inject(
            method = "shouldHideStatusEffectHud",
            at = @At("HEAD"),
            cancellable = true
    )
    private void devonian$onInventoryEffects(CallbackInfoReturnable<Boolean> cir) {
        if (!HideInventoryEffects.INSTANCE.isEnabled()) return;
        cir.setReturnValue(true);
    }
}
