package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.DisableNametagBackground;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
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
}
