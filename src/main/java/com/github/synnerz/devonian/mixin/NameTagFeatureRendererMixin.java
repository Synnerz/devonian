package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.DisableNametagBackground;
import com.github.synnerz.devonian.features.misc.NametagShadow;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(NameTagFeatureRenderer.class)
public class NameTagFeatureRendererMixin {
    @WrapOperation(
            method = "prepareText",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/Font;prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;"
            )
    )
    private static Font.PreparedText devonian$onNametagSubmit(Font instance, FormattedCharSequence text, float x, float y, int originalColor, boolean drawShadow, boolean includeEmpty, int backgroundColor, Operation<Font.PreparedText> original) {
        var bg = backgroundColor;
        var shadow = drawShadow;
        if (DisableNametagBackground.INSTANCE.isEnabled())
            bg = 0;
        if (NametagShadow.INSTANCE.isEnabled())
            shadow = true;

        return original.call(instance, text, x, y, originalColor, shadow, includeEmpty, bg);
    }
}
