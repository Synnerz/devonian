package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.DisableTextShadow;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Font.PreparedTextBuilder.class)
public class PreparedTextBuilderMixin {
    @Inject(
        method = "getShadowColor",
        at = @At("HEAD"),
        cancellable = true
    )
    private void devonian$disableTextShadow(Style style, int i, CallbackInfoReturnable<Integer> cir) {
        if (!DisableTextShadow.INSTANCE.isEnabled()) return;
        cir.setReturnValue(0);
    }
}
