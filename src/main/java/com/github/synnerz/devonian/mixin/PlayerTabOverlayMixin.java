package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.RemoveTabPing;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerTabOverlay.class)
public class PlayerTabOverlayMixin {
    @Inject(
            method = "renderPingIcon",
            at = @At("HEAD"),
            cancellable = true
    )
    private void devonian$renderPingIcon(GuiGraphics guiGraphics, int i, int j, int k, PlayerInfo playerInfo, CallbackInfo ci) {
        if (!RemoveTabPing.INSTANCE.isEnabled()) return;
        ci.cancel();
    }
}
