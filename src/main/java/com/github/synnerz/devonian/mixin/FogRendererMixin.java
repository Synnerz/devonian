package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.DisableFog;
import com.github.synnerz.devonian.features.misc.FixCrimsonIsleFog;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(net.minecraft.client.renderer.fog.FogRenderer.class)
public class FogRendererMixin {
    @ModifyVariable(
        method = "getBuffer",
        at = @At("HEAD"),
        ordinal = 0,
        argsOnly = true
    )
    private FogRenderer.FogMode devonian$disableFog(FogRenderer.FogMode fogMode) {
        if (!DisableFog.INSTANCE.isEnabled()) return fogMode;
        return FogRenderer.FogMode.NONE;
    }

    @WrapOperation(
            method = "computeFogColor",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GameRenderer;getNightVisionScale(Lnet/minecraft/world/entity/LivingEntity;F)F"
            )
    )
    private float devonian$onComputeFogColor(LivingEntity livingEntity, float f, Operation<Float> original) {
        if (!FixCrimsonIsleFog.INSTANCE.shouldFix()) return original.call(livingEntity, f);
        return 0.0F;
    }
}