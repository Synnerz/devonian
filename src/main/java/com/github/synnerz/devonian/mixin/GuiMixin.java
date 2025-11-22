package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.api.events.RenderOverlayEvent;
import com.github.synnerz.devonian.features.misc.DisableVanillaArmor;
import com.github.synnerz.devonian.features.misc.DisableVignette;
import com.github.synnerz.devonian.features.misc.HidePotionEffectOverlay;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {
    @Inject(
            method = "renderEffects",
            at = @At("HEAD"),
            cancellable = true
    )
    private void devonian$renderStatusOverlay(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!HidePotionEffectOverlay.INSTANCE.isEnabled()) return;
        ci.cancel();
    }

    @Inject(
            method = "render",
            at = @At("TAIL")
    )
    private void devonian$onRenderOverlay(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        new RenderOverlayEvent(guiGraphics, deltaTracker).post();
    }

    @Inject(
        method = "renderVignette(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/entity/Entity;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void devonian$disableVignette(GuiGraphics guiGraphics, Entity entity, CallbackInfo ci) {
        if (!DisableVignette.INSTANCE.isEnabled()) return;
        ci.cancel();
    }

    @Inject(
        method = "renderArmor",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void devonian$disableVanillaArmor(GuiGraphics guiGraphics, Player player, int i, int j, int k, int l, CallbackInfo ci) {
        if (!DisableVanillaArmor.INSTANCE.isEnabled()) return;
        ci.cancel();
    }
}
