package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.HideInventoryEffects;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.StatusEffectsDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StatusEffectsDisplay.class)
public class StatusEffectsDisplayMixin {
    @Inject(
            method = "drawStatusEffects(Lnet/minecraft/client/gui/DrawContext;II)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void devonian$onInventoryEffects(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
        if (!HideInventoryEffects.INSTANCE.isEnabled()) return;
        ci.cancel();
    }
}
