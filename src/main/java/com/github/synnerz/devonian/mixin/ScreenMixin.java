package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.Devonian;
import com.github.synnerz.devonian.api.events.PostRenderGuiEvent;
import com.github.synnerz.devonian.api.events.RenderGuiEvent;
import com.github.synnerz.devonian.features.misc.RemoveContainerBackground;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenMixin {
    @Inject(method = "renderTransparentBackground", at = @At("HEAD"), cancellable = true)
    private void devonian$renderTransparent(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (!RemoveContainerBackground.INSTANCE.isEnabled()) return;
        if (Devonian.INSTANCE.getMinecraft().level == null) return;
        ci.cancel();
    }

    @Inject(method = "renderWithTooltipAndSubtitles", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;nextStratum()V", ordinal = 0), cancellable = true)
    private void devonian$renderWithTooltip(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        Screen that = (Screen) (Object) this;

        if (new RenderGuiEvent(that, i, j, f, guiGraphics).post()) ci.cancel();
    }

    @Inject(method = "renderWithTooltipAndSubtitles", at = @At("TAIL"))
    private void devonian$postRenderWithTooltip(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        Screen that = (Screen) (Object) this;

        new PostRenderGuiEvent(that, i, j, f, guiGraphics).post();
    }
}
