package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.api.events.PostRenderHotbarSlotEvent;
import com.github.synnerz.devonian.api.events.RenderHotbarSlotEvent;
import com.github.synnerz.devonian.api.events.RenderOverlayEvent;
import com.github.synnerz.devonian.api.events.SelectedItemRenderEvent;
import com.github.synnerz.devonian.features.misc.*;
import com.github.synnerz.devonian.mixin.accessor.GuiGraphicsAccessor;
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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
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
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;renderBossOverlay(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V", shift = Shift.AFTER)
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
        if (HideHearts.INSTANCE.isEnabled()) {
            ci.cancel();
            return;
        }
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
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V", ordinal = 0)
    )
    private void devonian$centeredCrosshair(GuiGraphics instance, RenderPipeline renderPipeline, Identifier identifier, int i, int j, int k, int l, Operation<Void> original) {
        if (!CenteredCrosshair.INSTANCE.isEnabled()) {
            original.call(instance, renderPipeline, identifier, i, j, k, l);
            return;
        }
        GuiGraphicsAccessor accessor = (GuiGraphicsAccessor) instance;
        TextureAtlasSprite sprite = accessor.getGuiSprites().getSprite(identifier);
        AbstractTexture tex = minecraft.getTextureManager().getTexture(
            accessor.getGuiSprites().getSprite(identifier).atlasLocation()
        );
        GuiSpriteScaling scaling = GuiGraphicsAccessor.invokeSpriteScaling(sprite);
        TextureSetup texture = new TextureSetup(
            tex.getTextureView(), null, null,
            tex.getSampler(), null, null
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

    @Inject(
        method = "renderFood",
        at = @At("HEAD"),
        cancellable = true
    )
    private void devonian$disableHungerBar(GuiGraphics guiGraphics, Player player, int i, int j, CallbackInfo ci) {
        if (!DisableHungerBar.INSTANCE.isEnabled()) return;
        ci.cancel();
    }

    @Inject(
            method = "renderSelectedItemName",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawStringWithBackdrop(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIII)V"
            ),
            locals = LocalCapture.CAPTURE_FAILSOFT,
            cancellable = true
    )
    private void devonian$onRenderSelectedName(GuiGraphics guiGraphics, CallbackInfo ci, MutableComponent mutableComponent, int i, int j, int k, int l) {
        if (new SelectedItemRenderEvent(guiGraphics, mutableComponent).post())
            ci.cancel();
    }

    @Inject(
        method = "renderSlot",
        at = @At("HEAD"),
        cancellable = true
    )
    private void devonian$onRenderHotbarSlot(GuiGraphics guiGraphics, int i, int j, DeltaTracker deltaTracker, Player player, ItemStack itemStack, int k, CallbackInfo ci) {
        if (new RenderHotbarSlotEvent(itemStack, i, j, guiGraphics).post()) ci.cancel();
    }

    @Inject(
            method = "renderSlot",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V",
                    shift = Shift.AFTER
            )
    )
    private void devonian$onPostRenderHotbarSlot(GuiGraphics guiGraphics, int i, int j, DeltaTracker deltaTracker, Player player, ItemStack itemStack, int k, CallbackInfo ci) {
        new PostRenderHotbarSlotEvent(itemStack, i, j, guiGraphics).post();
    }

    @WrapOperation(
            method = "displayScoreboardSidebar",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V",
                    ordinal = 0
            )
    )
    private void devonian$sidebarTitle(GuiGraphics instance, int i, int j, int k, int l, int m, Operation<Void> original) {
        if (!CustomSidebarColor.INSTANCE.isEnabled()) {
            original.call(instance, i, j, k, l, m);
            return;
        }

        instance.fill(i, j, k, l, CustomSidebarColor.INSTANCE.getSETTING_TITLE_COLOR().get());
    }

    @WrapOperation(
            method = "displayScoreboardSidebar",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V",
                    ordinal = 1
            )
    )
    private void devonian$sidebarBody(GuiGraphics instance, int i, int j, int k, int l, int m, Operation<Void> original) {
        if (!CustomSidebarColor.INSTANCE.isEnabled()) {
            original.call(instance, i, j, k, l, m);
            return;
        }

        instance.fill(i, j, k, l, CustomSidebarColor.INSTANCE.getSETTING_BODY_COLOR().get());
    }

    @WrapOperation(
            method = "displayScoreboardSidebar",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V"
            )
    )
    private void devonian$sidebarDrawString(GuiGraphics instance, Font font, Component component, int i, int j, int k, boolean bl, Operation<Void> original) {
        if (!SidebarTextShadow.INSTANCE.isEnabled()) {
            original.call(instance, font, component, i, j, k, bl);
            return;
        }

        original.call(instance, font, component, i, j, k, true);
    }

    @Inject(
            method = "displayScoreboardSidebar",
            at = @At("HEAD"),
            cancellable = true
    )
    private void devonian$onScoreboardRender(GuiGraphics guiGraphics, Objective objective, CallbackInfo ci) {
        if (HideScoreboard.INSTANCE.isEnabled()) ci.cancel();
    }

    @Unique
    private boolean removeHypixel = false;

    @Inject(
        method = "method_55439",
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
            method = "renderItemHotbar",
            at = @At("HEAD"),
            cancellable = true
    )
    private void devonian$onRenderItemHotbar(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (HideHotbar.INSTANCE.isEnabled()) ci.cancel();
    }

    @Inject(
            method = "renderHotbarAndDecorations",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/contextualbar/ContextualBarRenderer;renderExperienceLevel(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;I)V"
            ),
            cancellable = true
    )
    private void devonian$onExperienceLevelRender(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (HideExperience.INSTANCE.isEnabled()) ci.cancel();
    }

    @Inject(
            method = "renderHotbarAndDecorations",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/contextualbar/ContextualBarRenderer;renderBackground(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V"
            ),
            cancellable = true
    )
    private void devonian$onExperienceBackgroundRender(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (HideExperience.INSTANCE.isEnabled()) ci.cancel();
    }

    @Inject(
            method = "renderHotbarAndDecorations",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/contextualbar/ContextualBarRenderer;render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V"
            ),
            cancellable = true
    )
    private void devonian$onExperienceBarRender(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (HideExperience.INSTANCE.isEnabled()) ci.cancel();
    }

    @WrapOperation(
            method = "renderChat",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/ChatComponent;render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;IIIZZ)V"
            )
    )
    private void devonian$onRenderChat(ChatComponent instance, GuiGraphics guiGraphics, Font font, int i, int j, int k, boolean bl, boolean bl2, Operation<Void> original) {
        original.call(instance, guiGraphics, font, i, j, k, PeekChatKeybind.INSTANCE.isEnabled() ? PeekChatKeybind.INSTANCE.getKeybind().isDown() : bl, bl2);
    }
}
