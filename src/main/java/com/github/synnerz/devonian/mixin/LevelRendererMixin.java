package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.api.events.RenderWorldEvent;
import com.github.synnerz.devonian.utils.render.Render3DImmediate;
import com.github.synnerz.devonian.utils.render.impl.Render3DState;
import com.github.synnerz.devonian.utils.render.impl.Render3DVertex;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Shadow
    @Final
    private LevelRenderState levelRenderState;

    @WrapOperation(
            method = "submitFeatures",
            at = @At(value = "NEW", target = "()Lcom/mojang/blaze3d/vertex/PoseStack;")
    )
    private PoseStack devonian$renderStart(Operation<PoseStack> original) {
        PoseStack ps = original.call();

        Render3DState.INSTANCE.setPoseStack(ps);
        Render3DState.INSTANCE.setCamera(levelRenderState.cameraRenderState);
        Render3DImmediate.INSTANCE.setPoseStack(ps);
        Render3DImmediate.INSTANCE.setCamera(levelRenderState.cameraRenderState);
        new RenderWorldEvent(levelRenderState).post();

        return ps;
    }

    @Inject(
        method = "lambda$addMainPass$0",
        at = @At("TAIL")
    )
    private void devonian$renderEnd(GpuBufferSlice terrainFog, LevelRenderState levelRenderState, ProfilerFiller profiler, ChunkSectionsToRender chunkSectionsToRender, ResourceHandle entityOutlineTarget, FeatureRenderDispatcher.PreparedFrame featureFrame, ResourceHandle translucentTarget, ResourceHandle mainTarget, ResourceHandle itemEntityTarget, ResourceHandle particleTarget, CallbackInfo ci) {
        Render3DVertex.INSTANCE.internalBatchedRender();
    }

    @Inject(
        method = "lambda$addAlwaysOnTopPass$0",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;executeAlwaysOnTop()V", shift = At.Shift.AFTER)
    )
    private void devonian$renderEndPhase(GpuBufferSlice fog, ResourceHandle mainTarget, FeatureRenderDispatcher.PreparedFrame featureFrame, CallbackInfo ci, @Local(name = "mainRenderTarget") RenderTarget mainRenderTarget) {
        Render3DVertex.INSTANCE.internalBatchedRenderPhase(mainRenderTarget);
    }

    @WrapOperation(
        method = "addAlwaysOnTopPass",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;hasAnyAlwaysOnTop()Z")
    )
    private boolean devonian$hasPhaseDraws(FeatureRenderDispatcher.PreparedFrame instance, Operation<Boolean> original) {
        return original.call(instance) || Render3DVertex.INSTANCE.internalHasPhaseRenders();
    }
}
