package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.DisableNametagBackground;
import com.github.synnerz.devonian.features.misc.NametagShadow;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(NameTagFeatureRenderer.class)
public class NameTagFeatureRendererMixin {
    @WrapOperation(
        method = "render",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeStorage$NameTagSubmit;backgroundColor()I")
    )
    private int devonian$disableNametagBackground(SubmitNodeStorage.NameTagSubmit instance, Operation<Integer> original) {
        if (!DisableNametagBackground.INSTANCE.isEnabled()) return original.call(instance);
        return 0;
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/Font;drawInBatch(Lnet/minecraft/network/chat/Component;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)V"
            )
    )
    private void devonian$NameTagShadow(Font instance, Component component, float f, float g, int i, boolean bl, Matrix4f matrix4f, MultiBufferSource multiBufferSource, Font.DisplayMode displayMode, int j, int k, Operation<Void> original) {
        if (!NametagShadow.INSTANCE.isEnabled()) {
            original.call(instance, component, f, g, i, bl, matrix4f, multiBufferSource, displayMode, j, k);
            return;
        }

        original.call(instance, component, f, g, i, true, matrix4f, multiBufferSource, displayMode, j, k);
    }
}
