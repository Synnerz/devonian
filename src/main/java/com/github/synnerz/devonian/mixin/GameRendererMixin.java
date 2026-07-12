package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.Devonian;
import com.github.synnerz.devonian.MouseHandlerAccessor;
import com.github.synnerz.devonian.api.events.GuiScaleEvent;
import com.github.synnerz.devonian.features.misc.NoHurtCamera;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.pip.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Shadow
    @Final
    private GuiRenderer guiRenderer;

    @Shadow
    @Final
    private FogRenderer fogRenderer;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private GameRenderState gameRenderState;

    @Shadow
    @Final
    private FeatureRenderDispatcher featureRenderDispatcher;

    @Shadow
    @Final
    private SubmitNodeStorage submitNodeStorage;
    @Unique
    GuiRenderer devonian$guiRenderer;

    @Unique
    GuiRenderState devonian$guiRenderState = new GuiRenderState();

    @Unique
    int devonian$Scaled = -1;

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void devonian$onHurtCam(CameraRenderState cameraState, PoseStack poseStack, CallbackInfo ci) {
        if (!NoHurtCamera.INSTANCE.isEnabled()) return;
        ci.cancel();
    }

    /**
     * - Taken from [<a href="https://codeberg.org/MicrocontrollersDev/Better-Screens/src/commit/cea229835ca9a9d07929ab346bc43e27580eb38d/src/main/java/dev/microcontrollers/betterscreens/mixin/GameRendererMixin.java">Microcontrollers' betterscreen</a>]
     */

    @Inject(method = "<init>", at = @At("TAIL"))
    private void devonian$onRenderInit(Minecraft minecraft, ItemInHandRenderer itemInHandRenderer, RenderBuffers renderBuffers, ModelManager modelManager, CallbackInfo ci, @Local(name = "atlasManager") AtlasManager atlasManager, @Local(name = "bufferSource") MultiBufferSource.BufferSource bufr) {
        devonian$guiRenderer = new GuiRenderer(
                devonian$guiRenderState,
                bufr,
                submitNodeStorage,
                featureRenderDispatcher,
                List.of(
                        new GuiEntityRenderer(bufr, minecraft.getEntityRenderDispatcher()),
                        new GuiSkinRenderer(bufr),
                        new GuiBookModelRenderer(bufr),
                        new GuiBannerResultRenderer(bufr, atlasManager),
                        new GuiProfilerChartRenderer(bufr)
                )
        );
    }

    @Inject(method = "close", at = @At("TAIL"))
    private void devonian$onClose(CallbackInfo ci) {
        devonian$guiRenderer.close();
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;endFrame()V"))
    private void devonian$onPostRender(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {
        if (devonian$Scaled == -1) return;

        var w = minecraft.getWindow();
        int scale = w.calculateScale(devonian$Scaled, minecraft.isEnforceUnicode());

        w.setGuiScale(scale);
        gameRenderState.windowRenderState.guiScale = w.getGuiScale();

        devonian$guiRenderer.render(this.fogRenderer.getBuffer(FogRenderer.FogMode.NONE));

        int oldScale = w.calculateScale(minecraft.options.guiScale().get(), minecraft.isEnforceUnicode());
        w.setGuiScale(oldScale);
        gameRenderState.windowRenderState.guiScale = w.getGuiScale();

        devonian$guiRenderer.endFrame();
        devonian$Scaled = -1;
    }

    @WrapOperation(
        method = "extractGui",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;extractRenderStateWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V")
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

            GuiRenderState scaledGuiRenderState = devonian$guiRenderState;

            w.setGuiScale(guiScale);

            mouseX = (int) mc.mouseHandler.getScaledXPos(w);
            mouseY = (int) mc.mouseHandler.getScaledYPos(w);
            mh.devonian$setGuiScaledWidthOverride(origW);
            mh.devonian$setGuiScaledHeightOverride(origH);

            instance.resize(w.getGuiScaledWidth(), w.getGuiScaledHeight());
            scaledGraphics = new GuiGraphicsExtractor(minecraft, scaledGuiRenderState, mouseX, mouseY);
            devonian$Scaled = scale;
        }

        original.call(instance, scaledGraphics, mouseX, mouseY, a);

        if (scale == -1) return;

        Window w = mc.getWindow();
        mh.devonian$setGuiScaledWidthOverride(w.getGuiScaledWidth());
        mh.devonian$setGuiScaledHeightOverride(w.getGuiScaledHeight());
        w.setGuiScale(oldScale);
    }
}
