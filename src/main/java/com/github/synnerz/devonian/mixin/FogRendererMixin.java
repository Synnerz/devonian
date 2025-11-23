package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.DisableFog;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.FogRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public class FogRendererMixin {
    @Inject(
        method = "setupFog",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void devonian$disableFog(Camera camera, FogRenderer.FogMode fogMode, Vector4f vector4f, float f, boolean bl, float g, CallbackInfoReturnable<FogParameters> cir) {
        if (!DisableFog.INSTANCE.isEnabled()) return;
        FogParameters val = DisableFog.INSTANCE.setupFog(camera, fogMode, vector4f, f, bl, g);
        if (val != null) cir.setReturnValue(val);
    }
}