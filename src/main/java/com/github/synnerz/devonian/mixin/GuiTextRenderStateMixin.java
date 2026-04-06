package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.GuiTextRenderStateAccessor;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.state.gui.GuiTextRenderState;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GuiTextRenderState.class)
public class GuiTextRenderStateMixin implements GuiTextRenderStateAccessor {
    @Unique
    private float xf = Float.NaN;
    @Unique
    private float yf = Float.NaN;

    @Override
    public float devonian$getXf() {
        return xf;
    }

    @Override
    public float devonian$getYf() {
        return yf;
    }

    @Override
    public void devonian$setXf(float x) {
        xf = x;
    }

    @Override
    public void devonian$setYf(float y) {
        yf = y;
    }

   @WrapOperation(
       method = "ensurePrepared",
       at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;")
   )
   private Font.PreparedText devonian$textFloatPosition(Font instance, FormattedCharSequence formattedCharSequence, float f, float g, int i, boolean bl, boolean bl2, int j, Operation<Font.PreparedText> original) {
       float x = Float.isNaN(xf) ? f : xf;
       float y = Float.isNaN(yf) ? g : yf;
       return instance.prepareText(formattedCharSequence, x, y, i, bl, bl2, j);
   }
}
