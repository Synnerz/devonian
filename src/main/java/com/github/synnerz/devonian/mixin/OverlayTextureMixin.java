package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.dungeons.m7.RecolorDragons;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OverlayTexture.class)
public class OverlayTextureMixin {
    @Shadow
    @Final
    private DynamicTexture texture;

    @Inject(
        method = "<init>",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/DynamicTexture;upload()V")
    )
    private void devonian$recolorDragons(CallbackInfo ci) {
        int[] cols = RecolorDragons.INSTANCE.getCOLORS();
        NativeImage img = texture.getPixels();
        assert img != null;
        for (int i = 0; i < cols.length; i++) {
            img.setPixel(15 - cols.length + i, 12, cols[i]);
        }
    }
}
