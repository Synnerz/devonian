package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.RemoveTabHead;
import com.github.synnerz.devonian.features.misc.RemoveTabPing;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerTabOverlay.class)
public class PlayerTabOverlayMixin {
    @Inject(
            method = "extractPingIcon",
            at = @At("HEAD"),
            cancellable = true
    )
    private void devonian$renderPingIcon(GuiGraphicsExtractor graphics, int slotWidth, int xo, int yo, PlayerInfo info, CallbackInfo ci) {
        if (!RemoveTabPing.INSTANCE.isEnabled()) return;
        ci.cancel();
    }

    @WrapOperation(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/PlayerFaceExtractor;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/resources/Identifier;IIIZZI)V"
            )
    )
    private void devonian$onTabPlayerRender(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int size, boolean hat, boolean flip, int color, Operation<Void> original) {
        if (!RemoveTabHead.INSTANCE.isEnabled()) {
            original.call(graphics, texture, x, y, size, hat, flip, color);
            return;
        }
        if (texture.getPath().equals("skins/f712b3a0a04da402d569cc02f3fcbc9228b2303a")) return;
        original.call(graphics, texture, x, y, size, hat, flip, color);
    }
}
