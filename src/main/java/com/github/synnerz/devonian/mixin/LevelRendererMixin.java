package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.api.events.RenderWorldEvent;
import com.github.synnerz.devonian.utils.render.Render3DImmediate;
import com.github.synnerz.devonian.utils.render.impl.Render3DState;
import com.github.synnerz.devonian.utils.render.impl.Render3DVertex;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.util.profiling.ProfilerFiller;
import org.joml.Matrix4fc;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @WrapOperation(
            method = "lambda$addMainPass$0",
            at = @At(value = "NEW", target = "()Lcom/mojang/blaze3d/vertex/PoseStack;")
    )
    private PoseStack devonian$renderStart(Operation<PoseStack> original, @Local(argsOnly = true, name = "levelRenderState") LevelRenderState levelRenderState) {
        PoseStack ps = original.call();

        Render3DState.INSTANCE.setPoseStack(ps);
        Render3DState.INSTANCE.setCamera(levelRenderState.cameraRenderState);
        Render3DImmediate.INSTANCE.setPoseStack(ps);
        Render3DImmediate.INSTANCE.setCamera(levelRenderState.cameraRenderState);
        new RenderWorldEvent(levelRenderState).post();

        return ps;
    }

    @WrapOperation(
        method = "lambda$addLateDebugPass$0",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/gizmos/DrawableGizmoPrimitives;isEmpty()Z")
    )
    private boolean devonian$hasPhaseRenders(DrawableGizmoPrimitives instance, Operation<Boolean> original) {
        return original.call(instance) && !Render3DVertex.INSTANCE.internalHasPhaseRenders();
    }

    @Inject(
        method = "lambda$addLateDebugPass$0",
        at = @At(value = "FIELD", target = "Lcom/mojang/blaze3d/systems/RenderSystem;outputColorTextureOverride:Lcom/mojang/blaze3d/textures/GpuTextureView;", opcode = Opcodes.PUTSTATIC, ordinal = 0)
    )
    private void devonian$renderEnd(GpuBufferSlice fog, ResourceHandle<RenderTarget> mainTarget, CameraRenderState camera, Matrix4fc modelViewMatrix, CallbackInfo ci) {
        Render3DVertex.INSTANCE.internalBatchedRender();
    }

    @Inject(
        method = "lambda$addLateDebugPass$0",
        at = @At(value = "FIELD", target = "Lcom/mojang/blaze3d/systems/RenderSystem;outputColorTextureOverride:Lcom/mojang/blaze3d/textures/GpuTextureView;", opcode = Opcodes.PUTSTATIC, ordinal = 1)
    )
    private void devonian$renderEndPhase(GpuBufferSlice fog, ResourceHandle<RenderTarget> mainTarget, CameraRenderState camera, Matrix4fc modelViewMatrix, CallbackInfo ci) {
        Render3DVertex.INSTANCE.internalBatchedRenderPhase(mainTarget.get());
    }
}
