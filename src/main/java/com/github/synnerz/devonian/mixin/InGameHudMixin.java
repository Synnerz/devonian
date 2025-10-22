package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.api.events.RenderOverlayEvent;
import com.github.synnerz.devonian.features.misc.HidePotionEffectOverlay;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Inject(
            method = "renderStatusEffectOverlay",
            at = @At("HEAD"),
            cancellable = true
    )
    private void devonian$renderStatusOverlay(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (!HidePotionEffectOverlay.INSTANCE.isEnabled()) return;
        ci.cancel();
    }

    @Inject(
            method = "render",
            at = @At("TAIL")
    )
    private void devonian$onRenderOverlay(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        new RenderOverlayEvent(context, tickCounter).post();
    }
}
