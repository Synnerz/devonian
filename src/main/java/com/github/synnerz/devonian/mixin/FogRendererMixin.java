package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.DisableFog;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(net.minecraft.client.renderer.fog.FogRenderer.class)
public class FogRendererMixin {
    @WrapOperation(
        method = "setupFog",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/fog/environment/FogEnvironment;isApplicable(Lnet/minecraft/world/level/material/FogType;Lnet/minecraft/world/entity/Entity;)Z")
    )
    private static boolean devonian$disableFog(FogEnvironment instance, FogType fogType, Entity entity, Operation<Boolean> original) {
        if (!DisableFog.INSTANCE.isEnabled()) return false;
        Boolean val = DisableFog.INSTANCE.setupFog(instance, fogType, entity);
        if (val == null) return original.call(instance, fogType, entity);
        return val;
    }
}