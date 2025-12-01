package com.github.synnerz.devonian.mixin;

import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Fixes empty signature console spam
@Mixin(YggdrasilMinecraftSessionService.class)
public class YggdrasilMinecraftSessionServiceMixin {
    @Redirect(
            method = "getPropertySignatureState",
            at = @At(value = "INVOKE", target = "Lcom/mojang/authlib/properties/Property;hasSignature()Z"),
            remap = false
    )
    private boolean devonian$hasSignature(Property instance) {
        boolean hasSig = instance.hasSignature();
        if (hasSig) {
            String signature = instance.signature();
            if (signature == null || signature.isEmpty()) return false;
        }

        return hasSig;
    }
}
