package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.api.events.RenderOverlayEvent;
import com.github.synnerz.devonian.features.misc.*;
import com.github.synnerz.devonian.mixin.accessor.GuiGraphicsAccessor;
import com.github.synnerz.devonian.utils.TexturedQuadRenderState;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.gui.GuiSpriteScaling;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix3x2f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(
        method = "renderEffects",
        at = @At("HEAD"),
        cancellable = true
    )
    private void devonian$renderStatusOverlay(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!HidePotionEffectOverlay.INSTANCE.isEnabled()) return;
        ci.cancel();
    }

    @Inject(
        method = "render",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;renderSleepOverlay(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V")
    )
    private void devonian$onRenderOverlay(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        new RenderOverlayEvent(guiGraphics, deltaTracker).post();
    }

    @Inject(
        method = "renderVignette(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/entity/Entity;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void devonian$disableVignette(GuiGraphics guiGraphics, Entity entity, CallbackInfo ci) {
        if (!DisableVignette.INSTANCE.isEnabled()) return;
        ci.cancel();
    }

    @Inject(
        method = "renderArmor",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void devonian$disableVanillaArmor(GuiGraphics guiGraphics, Player player, int i, int j, int k, int l, CallbackInfo ci) {
        if (!DisableVanillaArmor.INSTANCE.isEnabled()) return;
        ci.cancel();
    }

    @Inject(
        method = "renderHearts",
        at = @At("HEAD"),
        cancellable = true
    )
    private void devonian$accurateAbsorption(
        GuiGraphics guiGraphics, Player player,
        int left, int top,
        int rowGap, int regen,
        float maxHearts, int hearts, int displayHearts, int absorption,
        boolean blinking,
        CallbackInfo ci
    ) {
        if (!AccurateAbsorption.INSTANCE.isEnabled()) return;
        AccurateAbsorption.INSTANCE.renderHearts(
            (Gui) (Object) this,
            guiGraphics, player,
            left, top,
            rowGap, regen,
            maxHearts, hearts, displayHearts, absorption,
            blinking
        );
        ci.cancel();
    }

    @WrapOperation(
        method = "renderCrosshair",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/CameraType;isFirstPerson()Z")
    )
    private boolean devonian$thirdPersonCrosshair(CameraType instance, Operation<Boolean> original) {
        if (!ThirdPersonCrosshair.INSTANCE.isEnabled()) return original.call(instance);
        return true;
    }

    @WrapOperation(
        method = "renderCrosshair",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/ResourceLocation;IIII)V", ordinal = 0)
    )
    private void devonian$centeredCrosshair(GuiGraphics instance, RenderPipeline renderPipeline, ResourceLocation resourceLocation, int i, int j, int k, int l, Operation<Void> original) {
        if (!CenteredCrosshair.INSTANCE.isEnabled()) {
            original.call(instance, renderPipeline, resourceLocation, i, j, k, l);
            return;
        }
        GuiGraphicsAccessor accessor = (GuiGraphicsAccessor) instance;
        TextureAtlasSprite sprite = accessor.getGuiSprites().getSprite(resourceLocation);
        GuiSpriteScaling scaling = accessor.invokeSpriteScaling(sprite);
        TextureSetup texture = TextureSetup.singleTexture(
            minecraft.getTextureManager().getTexture(
                accessor.getGuiSprites().getSprite(resourceLocation).atlasLocation()
            ).getTextureView()
        );
        Matrix3x2f mat = new Matrix3x2f(instance.pose());

        instance.guiRenderState.submitGuiElement(
            new TexturedQuadRenderState(
                renderPipeline,
                texture,
                mat,
                (instance.guiWidth() - 15) / 2f,
                (instance.guiHeight() - 15) / 2f,
                (instance.guiWidth() + 15) / 2f,
                (instance.guiHeight() + 15) / 2f,
                sprite.getU0(), sprite.getV0(),
                sprite.getU1(), sprite.getV1(),
                -1,
                instance.scissorStack.peek()
            )
        );
    }
}
