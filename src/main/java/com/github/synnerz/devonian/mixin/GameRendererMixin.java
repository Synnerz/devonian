package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.Devonian;
import com.github.synnerz.devonian.MouseHandlerAccessor;
import com.github.synnerz.devonian.api.events.GuiScaleEvent;
import com.github.synnerz.devonian.features.misc.NoHurtCamera;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Shadow
    @Final
    private GuiRenderer guiRenderer;

    @Shadow
    @Final
    private FogRenderer fogRenderer;

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void devonian$onHurtCam(PoseStack poseStack, float f, CallbackInfo ci) {
        if (!NoHurtCamera.INSTANCE.isEnabled()) return;
        ci.cancel();
    }

    @WrapOperation(
        method = "render",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;renderWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphics;IIF)V")
    )
    private void devonian$guiScaleEventPost(Screen instance, GuiGraphics guiGraphics, int i, int j, float f, Operation<Void> original) {
        Minecraft mc = Devonian.INSTANCE.getMinecraft();

        GuiScaleEvent evn = new GuiScaleEvent(instance);
        evn.post();

        int scale = evn.getOverrideScale();
        int oldScale = -1;
        MouseHandlerAccessor mh = (MouseHandlerAccessor) mc.mouseHandler;
        mh.devonian$setGuiScaledWidthOverride(-1);
        mh.devonian$setGuiScaledHeightOverride(-1);
        if (scale != -1) {
            guiRenderer.render(fogRenderer.getBuffer(FogRenderer.FogMode.NONE));
            Window w = mc.getWindow();

            oldScale = w.getGuiScale();
            int guiScale = w.calculateScale(scale, mc.isEnforceUnicode());

            int origW = w.getGuiScaledWidth();
            int origH = w.getGuiScaledHeight();

            w.setGuiScale(guiScale);

            i = (int) mc.mouseHandler.getScaledXPos(w);
            j = (int) mc.mouseHandler.getScaledYPos(w);
            mh.devonian$setGuiScaledWidthOverride(origW);
            mh.devonian$setGuiScaledHeightOverride(origH);

            instance.resize(mc, w.getGuiScaledWidth(), w.getGuiScaledHeight());
        }

        original.call(instance, guiGraphics, i, j, f);

        if (scale == -1) return;

        Window w = mc.getWindow();
        guiRenderer.render(fogRenderer.getBuffer(FogRenderer.FogMode.NONE));

        mh.devonian$setGuiScaledWidthOverride(w.getGuiScaledWidth());
        mh.devonian$setGuiScaledHeightOverride(w.getGuiScaledHeight());
        w.setGuiScale(oldScale);
        // instance.resize(mc, w.getGuiScaledWidth(), w.getGuiScaledHeight());
    }
}
