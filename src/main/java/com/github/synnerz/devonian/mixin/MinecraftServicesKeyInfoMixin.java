package com.github.synnerz.devonian.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.services.MinecraftServicesKeyInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.security.Signature;

// Fixes empty signature console spam
@Mixin(MinecraftServicesKeyInfo.class)
public class MinecraftServicesKeyInfoMixin {
    @WrapOperation(method = "validateProperty", at = @At(value = "INVOKE", target = "Ljava/security/Signature;verify([B)Z"))
    private boolean devonian$hasSignature(Signature instance, byte[] signature, Operation<Boolean> original) {
        if (signature.length != 0) return original.call(instance, signature);

        return false;
    }
}
