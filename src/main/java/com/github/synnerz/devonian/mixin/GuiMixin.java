package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.api.events.PostRenderHotbarSlotEvent;
import com.github.synnerz.devonian.api.events.RenderHotbarSlotEvent;
import com.github.synnerz.devonian.api.events.RenderOverlayEvent;
import com.github.synnerz.devonian.api.events.SelectedItemRenderEvent;
import com.github.synnerz.devonian.features.misc.*;
import com.github.synnerz.devonian.mixin.accessor.GuiGraphicsExtractorAccessor;
import com.github.synnerz.devonian.utils.render.states.TexturedQuadRenderState;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.contextualbar.ContextualBarRenderer;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.gui.GuiSpriteScaling;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.Scoreboard;
import org.joml.Matrix3x2f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Arrays;
import java.util.function.IntFunction;
import java.util.stream.Stream;

@Mixin(Gui.class)
public class GuiMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(
        method = "extractEffects",
        at = @At("HEAD"),
        cancellable = true
    )
    private void devonian$renderStatusOverlay(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!HidePotionEffectOverlay.INSTANCE.isEnabled()) return;
        ci.cancel();
    }

    @Inject(
        method = "extractRenderState",
        at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/client/gui/Gui;extractBossOverlay(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
                shift = Shift.AFTER
        )
    )
    private void devonian$onRenderOverlay(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        new RenderOverlayEvent(graphics, deltaTracker).post();
    }

    @Inject(
        method = "extractVignette",
        at = @At("HEAD"),
        cancellable = true
    )
    private void devonian$disableVignette(GuiGraphicsExtractor graphics, Entity camera, CallbackInfo ci) {
        if (!DisableVignette.INSTANCE.isEnabled()) return;
        ci.cancel();
    }

    @Inject(
        method = "extractArmor",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void devonian$disableVanillaArmor(GuiGraphicsExtractor graphics, Player player, int yLineBase, int numHealthRows, int healthRowHeight, int xLeft, CallbackInfo ci) {
        if (!DisableVanillaArmor.INSTANCE.isEnabled()) return;
        ci.cancel();
    }

    @Inject(
        method = "extractHearts",
        at = @At("HEAD"),
        cancellable = true
    )
    private void devonian$accurateAbsorption(
            GuiGraphicsExtractor graphics, Player player,
            int xLeft, int yLineBase, int healthRowHeight,
            int heartOffsetIndex, float maxHealth, int currentHealth,
            int oldHealth, int absorption, boolean blink, CallbackInfo ci
    ) {
        if (HideHearts.INSTANCE.isEnabled()) {
            ci.cancel();
            return;
        }
        if (!AccurateAbsorption.INSTANCE.isEnabled()) return;
        AccurateAbsorption.INSTANCE.renderHearts(
            (Gui) (Object) this,
            graphics, player,
            xLeft, yLineBase,
            healthRowHeight, heartOffsetIndex,
            maxHealth, currentHealth, oldHealth, absorption,
            blink
        );
        ci.cancel();
    }

    @WrapOperation(
        method = "extractCrosshair",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/CameraType;isFirstPerson()Z")
    )
    private boolean devonian$thirdPersonCrosshair(CameraType instance, Operation<Boolean> original) {
        if (!ThirdPersonCrosshair.INSTANCE.isEnabled()) return original.call(instance);
        return true;
    }

    @WrapOperation(
        method = "extractCrosshair",
        at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V",
                ordinal = 0
        )
    )
    private void devonian$centeredCrosshair(
            GuiGraphicsExtractor instance, RenderPipeline renderPipeline, Identifier location, int x, int y, int width, int height, Operation<Void> original
    ) {
        if (!CenteredCrosshair.INSTANCE.isEnabled()) {
            original.call(instance, renderPipeline, location, x, y, width, height);
            return;
        }
        GuiGraphicsExtractorAccessor accessor = (GuiGraphicsExtractorAccessor) instance;
        TextureAtlasSprite sprite = accessor.getGuiSprites().getSprite(location);
        AbstractTexture tex = minecraft.getTextureManager().getTexture(
            accessor.getGuiSprites().getSprite(location).atlasLocation()
        );
        GuiSpriteScaling scaling = GuiGraphicsExtractorAccessor.invokeSpriteScaling(sprite);
        TextureSetup texture = new TextureSetup(
            tex.getTextureView(), null, null,
            tex.getSampler(), null, null
        );
        Matrix3x2f mat = new Matrix3x2f(instance.pose());

        instance.guiRenderState.addGuiElement(
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

    @Inject(
        method = "extractFood",
        at = @At("HEAD"),
        cancellable = true
    )
    private void devonian$disableHungerBar(GuiGraphicsExtractor graphics, Player player, int yLineBase, int xRight, CallbackInfo ci) {
        if (!DisableHungerBar.INSTANCE.isEnabled()) return;
        ci.cancel();
    }

    @Inject(
            method = "extractSelectedItemName",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;textWithBackdrop(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIII)V"
            ),
            locals = LocalCapture.CAPTURE_FAILSOFT,
            cancellable = true
    )
    private void devonian$onRenderSelectedName(GuiGraphicsExtractor graphics, CallbackInfo ci, MutableComponent str, int strWidth, int x, int y, int alpha) {
        if (new SelectedItemRenderEvent(graphics, str).post())
            ci.cancel();
    }

    @Inject(
        method = "extractSlot",
        at = @At("HEAD"),
        cancellable = true
    )
    private void devonian$onRenderHotbarSlot(GuiGraphicsExtractor graphics, int x, int y, DeltaTracker deltaTracker, Player player, ItemStack itemStack, int seed, CallbackInfo ci) {
        if (new RenderHotbarSlotEvent(itemStack, x, y, graphics).post()) ci.cancel();
    }

    @Inject(
            method = "extractSlot",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V",
                    shift = Shift.AFTER
            )
    )
    private void devonian$onPostRenderHotbarSlot(GuiGraphicsExtractor graphics, int x, int y, DeltaTracker deltaTracker, Player player, ItemStack itemStack, int seed, CallbackInfo ci) {
        new PostRenderHotbarSlotEvent(itemStack, x, y, graphics).post();
    }

    @WrapOperation(
            method = "displayScoreboardSidebar",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V",
                    ordinal = 0
            )
    )
    private void devonian$sidebarTitle(GuiGraphicsExtractor instance, int x0, int y0, int x1, int y1, int col, Operation<Void> original) {
        if (!CustomSidebarColor.INSTANCE.isEnabled()) {
            original.call(instance, x0, y0, x1, y1, col);
            return;
        }

        instance.fill(x0, y0, x1, y1, CustomSidebarColor.INSTANCE.getSETTING_TITLE_COLOR().get());
    }

    @WrapOperation(
            method = "displayScoreboardSidebar",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V",
                    ordinal = 1
            )
    )
    private void devonian$sidebarBody(GuiGraphicsExtractor instance, int x0, int y0, int x1, int y1, int col, Operation<Void> original) {
        if (!CustomSidebarColor.INSTANCE.isEnabled()) {
            original.call(instance, x0, y0, x1, y1, col);
            return;
        }

        instance.fill(x0, y0, x1, y1, CustomSidebarColor.INSTANCE.getSETTING_BODY_COLOR().get());
    }

    @WrapOperation(
            method = "displayScoreboardSidebar",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V"
            )
    )
    private void devonian$sidebarDrawString(GuiGraphicsExtractor instance, Font font, Component str, int x, int y, int color, boolean dropShadow, Operation<Void> original) {
        if (!SidebarTextShadow.INSTANCE.isEnabled()) {
            original.call(instance, font, str, x, y, color, dropShadow);
            return;
        }

        original.call(instance, font, str, x, y, color, true);
    }

    @Inject(
            method = "displayScoreboardSidebar",
            at = @At("HEAD"),
            cancellable = true
    )
    private void devonian$onScoreboardRender(GuiGraphicsExtractor graphics, Objective objective, CallbackInfo ci) {
        if (HideScoreboard.INSTANCE.isEnabled()) ci.cancel();
    }

    @Unique
    private boolean removeHypixel = false;

    @Inject(
        method = "lambda$displayScoreboardSidebar$1",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/scores/PlayerScoreEntry;formatValue(Lnet/minecraft/network/chat/numbers/NumberFormat;)Lnet/minecraft/network/chat/MutableComponent;")
    )
    private void devonian$removeHypixelScoreboard(Scoreboard scoreboard, NumberFormat numberFormat, PlayerScoreEntry playerScoreEntry, CallbackInfoReturnable cir, @Local(ordinal = 1) Component component2) {
        if (!RemoveHypixelScoreboard.INSTANCE.isEnabled()) return;
        if (component2.getString().startsWith("§ewww.hypixel")) removeHypixel = true;
    }

    @WrapOperation(
        method = "displayScoreboardSidebar",
        at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;toArray(Ljava/util/function/IntFunction;)[Ljava/lang/Object;")
    )
    private Object[] devonian$removeHypixelScoreboard(Stream instance, IntFunction<Object[]> intFunction, Operation<Object[]> original) {
        Object[] arr = original.call(instance, intFunction);
        if (removeHypixel) {
            arr = Arrays.copyOfRange(arr, 0, Math.max(0, arr.length - 2));
            removeHypixel = false;
        }
        return arr;
    }

    @Inject(
            method = "extractItemHotbar",
            at = @At("HEAD"),
            cancellable = true
    )
    private void devonian$onRenderItemHotbar(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (HideHotbar.INSTANCE.isEnabled()) ci.cancel();
    }

    @WrapOperation(
            method = "extractHotbarAndDecorations",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/contextualbar/ContextualBarRenderer;extractExperienceLevel(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;I)V"
            )
    )
    private void devonian$onExperienceLevelRender(GuiGraphicsExtractor graphics, Font font, int experienceLevel, Operation<Void> original) {
        if (HideExperience.INSTANCE.isEnabled() && HideExperience.INSTANCE.getSETTING_REMOVE_LEVEL().get()) return;
        original.call(graphics, font, experienceLevel);
    }

    @WrapOperation(
            method = "extractHotbarAndDecorations",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/contextualbar/ContextualBarRenderer;extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"
            )
    )
    private void devonian$onExperienceBackgroundRender(ContextualBarRenderer instance, GuiGraphicsExtractor guiGraphicsExtractor, DeltaTracker deltaTracker, Operation<Void> original) {
        if (HideExperience.INSTANCE.isEnabled() && HideExperience.INSTANCE.getSETTING_REMOVE_BAR().get()) return;
        original.call(instance, guiGraphicsExtractor, deltaTracker);
    }

    @WrapOperation(
            method = "extractChat",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/ChatComponent;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V"
            )
    )
    private void devonian$onRenderChat(ChatComponent instance, GuiGraphicsExtractor graphics, Font font, int ticks, int mouseX, int mouseY, ChatComponent.DisplayMode displayMode, boolean changeCursorOnInsertions, Operation<Void> original) {
        original.call(
                instance, graphics,
                font, ticks,
                mouseX, mouseY,
                PeekChatKeybind.INSTANCE.isEnabled() && PeekChatKeybind.INSTANCE.getKeybind().isDown()
                        ? ChatComponent.DisplayMode.FOREGROUND
                        : displayMode,
                changeCursorOnInsertions
        );
    }
}
