package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.RemoveTabHead;
import com.github.synnerz.devonian.features.misc.RemoveTabPing;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.ResourceLocation;
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

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/PlayerFaceRenderer;draw(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/resources/ResourceLocation;IIIZZI)V"
            )
    )
    private void devonian$onTabPlayerRender(GuiGraphics guiGraphics, ResourceLocation resourceLocation, int i, int j, int k, boolean bl, boolean bl2, int l, Operation<Void> original) {
        if (!RemoveTabHead.INSTANCE.isEnabled()) {
            original.call(guiGraphics, resourceLocation, i, j, k, bl, bl2, l);
            return;
        }
        if (resourceLocation.getPath().equals("skins/f712b3a0a04da402d569cc02f3fcbc9228b2303a")) return;
        original.call(guiGraphics, resourceLocation, i, j, k, bl, bl2, l);
    }
}
