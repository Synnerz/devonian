package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.api.events.RenderWorldEvent;
import com.github.synnerz.devonian.utils.render.Render3DImmediate;
import com.github.synnerz.devonian.utils.render.impl.Render3DState;
import com.github.synnerz.devonian.utils.render.impl.Render3DVertex;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.level.LevelRenderState;
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

    @Definition(id = "hasAlwaysOnTopGizmos", local = @Local(type = boolean.class, name = "hasAlwaysOnTopGizmos", argsOnly = true))
    @Expression("hasAlwaysOnTopGizmos")
    @ModifyExpressionValue(
        method = "lambda$addMainPass$0",
        at = @At(value = "MIXINEXTRAS:EXPRESSION")
    )
    private boolean devonian$renderEnd(boolean original, @Local(name = "mainTarget") RenderTarget mainTarget) {
        assert mainTarget.getColorTextureView() != null;
        assert mainTarget.getDepthTextureView() != null;
        Render3DVertex.INSTANCE.internalBatchedRender(mainTarget.getColorTextureView(), mainTarget.getDepthTextureView());
        return original || Render3DVertex.INSTANCE.internalHasPhaseRenders();
    }

    @Inject(
        method = "executeAlwaysOnTop",
        at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/commands/RenderPass;close()V", ordinal = 0, shift = At.Shift.AFTER)
    )
    private void devonian$renderEndPhase(FeatureRenderDispatcher.PreparedFrame featureFrame, RenderTarget mainTarget, boolean consistentDepthRequired, CallbackInfo ci, @Local(name = "depthTextureView") GpuTextureView depthTextureView) {
        assert mainTarget.getColorTextureView() != null;
        Render3DVertex.INSTANCE.internalBatchedRenderPhase(mainTarget.getColorTextureView(), depthTextureView);
    }
}
