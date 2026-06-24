package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.Devonian;
import com.github.synnerz.devonian.MouseHandlerAccessor;
import com.github.synnerz.devonian.api.events.*;
import com.github.synnerz.devonian.features.misc.*;
import com.github.synnerz.devonian.mixin.accessor.GameRendererAccessor;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    public abstract @Nullable Screen screen();

    @WrapOperation(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/Screen;extractRenderStateWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"
            )
    )
    private void devonian$guiScaleEventPost(Screen instance, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, Operation<Void> original) {
        Minecraft mc = Devonian.INSTANCE.getMinecraft();

        GuiScaleEvent evn = new GuiScaleEvent(instance);
        evn.post();

        int scale = evn.getOverrideScale();
        int oldScale = -1;
        MouseHandlerAccessor mh = (MouseHandlerAccessor) mc.mouseHandler;
        mh.devonian$setGuiScaledWidthOverride(-1);
        mh.devonian$setGuiScaledHeightOverride(-1);
        if (scale != -1) {
            ((GameRendererAccessor) minecraft.gameRenderer).getGuiRenderer().render();
            Window w = mc.getWindow();

            oldScale = w.getGuiScale();
            int guiScale = w.calculateScale(scale, mc.isEnforceUnicode());

            int origW = w.getGuiScaledWidth();
            int origH = w.getGuiScaledHeight();

            w.setGuiScale(guiScale);

            mouseX = (int) mc.mouseHandler.getScaledXPos(w);
            mouseY = (int) mc.mouseHandler.getScaledYPos(w);
            mh.devonian$setGuiScaledWidthOverride(origW);
            mh.devonian$setGuiScaledHeightOverride(origH);

            instance.resize(w.getGuiScaledWidth(), w.getGuiScaledHeight());
        }

        original.call(instance, graphics, mouseX, mouseY, a);

        if (scale == -1) return;

        Window w = mc.getWindow();
        ((GameRendererAccessor) minecraft.gameRenderer).getGuiRenderer().render();

        mh.devonian$setGuiScaledWidthOverride(w.getGuiScaledWidth());
        mh.devonian$setGuiScaledHeightOverride(w.getGuiScaledHeight());
        w.setGuiScale(oldScale);
        // instance.resize(mc, w.getGuiScaledWidth(), w.getGuiScaledHeight());
    }

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void devonian$onSetScreen(Screen screen, CallbackInfo ci) {
        if (screen == null && screen() != null && new GuiCloseEvent(screen()).post()) ci.cancel();
        if (screen != null && new GuiOpenEvent(screen).post()) ci.cancel();
    }
}
