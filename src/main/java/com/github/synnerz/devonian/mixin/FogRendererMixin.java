package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.DisableFog;
import net.minecraft.client.renderer.fog.FogRenderer;
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
    private static FogRenderer.FogMode devonian$disableFog(FogRenderer.FogMode fogMode) {
        if (!DisableFog.INSTANCE.isEnabled()) return fogMode;
        return FogRenderer.FogMode.NONE;
    }
}