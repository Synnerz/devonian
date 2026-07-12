package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.GameRendererScaleAccessor;
import com.github.synnerz.devonian.features.misc.NoHurtCamera;
import com.github.synnerz.devonian.features.misc.inventory.InventoryScale;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.pip.*;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
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
public class GameRendererMixin implements GameRendererScaleAccessor {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private GameRenderState gameRenderState;

    @Shadow
    @Final
    private FeatureRenderDispatcher featureRenderDispatcher;

    @Unique
    GuiRenderer devonian$guiRenderer;

    @Unique
    GuiRenderState devonian$guiRenderState = new GuiRenderState();

    @Unique
    boolean devonian$Scaled = false;

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void devonian$onHurtCam(CameraRenderState cameraState, PoseStack poseStack, CallbackInfo ci) {
        if (!NoHurtCamera.INSTANCE.isEnabled()) return;
        ci.cancel();
    }

    /**
     * - Taken from [<a href="https://codeberg.org/MicrocontrollersDev/Better-Screens/src/commit/cea229835ca9a9d07929ab346bc43e27580eb38d/src/main/java/dev/microcontrollers/betterscreens/mixin/GameRendererMixin.java">Microcontrollers' betterscreen</a>]
     */

    @Inject(method = "<init>", at = @At("TAIL"))
    private void devonian$onRenderInit(Minecraft minecraft, ItemInHandRenderer itemInHandRenderer, ModelManager modelManager, CallbackInfo ci, @Local(name = "atlasManager") AtlasManager atlasManager) {
        devonian$guiRenderer = new GuiRenderer(
                devonian$guiRenderState,
                featureRenderDispatcher,
                List.of(
                        new GuiEntityRenderer(minecraft.getEntityRenderDispatcher()),
                        new GuiSkinRenderer(),
                        new GuiBookModelRenderer(),
                        new GuiBannerResultRenderer(atlasManager),
                        new GuiProfilerChartRenderer()
                )
        );
    }

    @Inject(method = "close", at = @At("TAIL"))
    private void devonian$onClose(CallbackInfo ci) {
        devonian$guiRenderer.close();
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;endFrame()V"))
    private void devonian$onPostRender(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {
        if (!devonian$Scaled) return;

        var w = minecraft.getWindow();
        int scale = w.calculateScale(InventoryScale.INSTANCE.getScale(), minecraft.isEnforceUnicode());

        w.setGuiScale(scale);
        gameRenderState.windowRenderState.guiScale = w.getGuiScale();

        devonian$guiRenderer.render();

        int oldScale = w.calculateScale(minecraft.options.guiScale().get(), minecraft.isEnforceUnicode());
        w.setGuiScale(oldScale);
        gameRenderState.windowRenderState.guiScale = w.getGuiScale();

        devonian$guiRenderer.endFrame();
        devonian$Scaled = false;
    }

    @Override
    public GuiRenderState devonian$guiRenderState() {
        return devonian$guiRenderState;
    }

    @Override
    public void devonian$setScaled(boolean scale) {
        devonian$Scaled = scale;
    }

    @Override
    public boolean devonian$getScaled() {
        return devonian$Scaled;
    }
}
