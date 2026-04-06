package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.utils.render.Render3DImmediate;
import com.github.synnerz.devonian.utils.render.impl.Render3DState;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    // FIXME
//    @WrapOperation(
//        method = "method_62214",
//        at = @At(value = "NEW", target = "()Lcom/mojang/blaze3d/vertex/PoseStack;")
//    )
//    private PoseStack devonian$render3D(Operation<PoseStack> original) {
//        PoseStack ps = original.call();
//
//        Render3DState.INSTANCE.setPoseStack(ps);
//        Render3DImmediate.INSTANCE.setPoseStack(ps);
//
//        return ps;
//    }
}
