package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.Devonian;
import com.github.synnerz.devonian.MouseHandlerAccessor;
import com.github.synnerz.devonian.GameRendererScaleAccessor;
import com.github.synnerz.devonian.api.events.*;
import com.github.synnerz.devonian.features.misc.inventory.InventoryScale;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
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
        GuiGraphicsExtractor scaledGraphics = graphics;

        GuiScaleEvent evn = new GuiScaleEvent(instance);
        evn.post();

        int scale = evn.getOverrideScale();
        int oldScale = -1;
        MouseHandlerAccessor mh = (MouseHandlerAccessor) mc.mouseHandler;
        mh.devonian$setGuiScaledWidthOverride(-1);
        mh.devonian$setGuiScaledHeightOverride(-1);
        if (scale != -1) {
            Window w = mc.getWindow();

            oldScale = w.getGuiScale();
            int guiScale = w.calculateScale(scale, mc.isEnforceUnicode());

            int origW = w.getGuiScaledWidth();
            int origH = w.getGuiScaledHeight();

            GuiRenderState scaledGuiRenderState = ((GameRendererScaleAccessor) minecraft.gameRenderer).devonian$guiRenderState();

            w.setGuiScale(guiScale);

            mouseX = (int) mc.mouseHandler.getScaledXPos(w);
            mouseY = (int) mc.mouseHandler.getScaledYPos(w);
            mh.devonian$setGuiScaledWidthOverride(origW);
            mh.devonian$setGuiScaledHeightOverride(origH);

            instance.resize(w.getGuiScaledWidth(), w.getGuiScaledHeight());
            scaledGraphics = new GuiGraphicsExtractor(minecraft, scaledGuiRenderState, mouseX, mouseY);
            ((GameRendererScaleAccessor) minecraft.gameRenderer).devonian$setScaled(true);
        }

        original.call(instance, scaledGraphics, mouseX, mouseY, a);

        if (scale == -1) return;

        Window w = mc.getWindow();
        mh.devonian$setGuiScaledWidthOverride(w.getGuiScaledWidth());
        mh.devonian$setGuiScaledHeightOverride(w.getGuiScaledHeight());
        w.setGuiScale(oldScale);
    }

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void devonian$onSetScreen(Screen screen, CallbackInfo ci) {
        if (screen == null && screen() != null && new GuiCloseEvent(screen()).post()) ci.cancel();
        if (screen != null && new GuiOpenEvent(screen).post()) ci.cancel();
    }
}
