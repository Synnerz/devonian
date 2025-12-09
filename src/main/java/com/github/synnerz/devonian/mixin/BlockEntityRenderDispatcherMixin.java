package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.api.events.PostRenderTileEntityEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRenderDispatcherMixin {
    @Inject(
        method = "submit",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderer;submit(Lnet/minecraft/client/renderer/blockentity/state/BlockEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V", shift = Shift.AFTER)
    )
    private void devonian$postRenderTileEntity(BlockEntityRenderState blockEntityRenderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, CallbackInfo ci) {
        new PostRenderTileEntityEvent(blockEntityRenderState, cameraRenderState, poseStack, submitNodeCollector).post();
    }
}
