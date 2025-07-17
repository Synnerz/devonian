package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.RemoveTabPing;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {
    @Inject(
            method = "renderLatencyIcon",
            at = @At("HEAD"),
            cancellable = true
    )
    private void devonian$renderPingIcon(DrawContext context, int width, int x, int y, PlayerListEntry entry, CallbackInfo ci) {
        if (!RemoveTabPing.INSTANCE.isEnabled()) return;
        ci.cancel();
    }
}
