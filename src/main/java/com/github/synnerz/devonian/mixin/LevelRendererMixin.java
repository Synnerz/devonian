package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.api.events.RenderWorldEvent;
import com.github.synnerz.devonian.utils.render.Render3DImmediate;
import com.github.synnerz.devonian.utils.render.impl.Render3DState;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @WrapOperation(
            method = "lambda$addMainPass$0",
            at = @At(value = "NEW", target = "()Lcom/mojang/blaze3d/vertex/PoseStack;")
    )
    private PoseStack devonian$render3D(Operation<PoseStack> original, @Local(argsOnly = true, name = "levelRenderState") LevelRenderState levelRenderState) {
        PoseStack ps = original.call();

        Render3DState.INSTANCE.setPoseStack(ps);
        Render3DState.INSTANCE.setCamera(levelRenderState.cameraRenderState);
        Render3DImmediate.INSTANCE.setPoseStack(ps);
        Render3DImmediate.INSTANCE.setCamera(levelRenderState.cameraRenderState);
        new RenderWorldEvent(levelRenderState).post();

        return ps;
    }
}
