package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.DisableSuffocatingOverlay;
import com.github.synnerz.devonian.features.misc.DisableWaterOverlay;
import com.github.synnerz.devonian.features.misc.RemoveFireOverlay;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public class ScreenEffectRendererMixin {
    @Inject(
            method = "renderFire",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void devonian$renderFireOverlay(PoseStack poseStack, MultiBufferSource multiBufferSource, TextureAtlasSprite textureAtlasSprite, CallbackInfo ci) {
        if (!RemoveFireOverlay.INSTANCE.isEnabled()) return;
        ci.cancel();
    }

    @Inject(
        method = "renderWater",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void devonian$disableWaterOverlay(Minecraft minecraft, PoseStack poseStack, MultiBufferSource multiBufferSource, CallbackInfo ci) {
        if (!DisableWaterOverlay.INSTANCE.isEnabled()) return;
        ci.cancel();
    }

    @WrapOperation(
        method = "renderScreenEffect",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ScreenEffectRenderer;getViewBlockingState(Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/level/block/state/BlockState;")
    )
    private static BlockState devonian$disableSuffocatingOverlay(Player player, Operation<BlockState> original) {
        if (!DisableSuffocatingOverlay.INSTANCE.isEnabled()) return original.call(player);
        return null;
    }
}
