package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.FixObfuscatedText;
import com.github.synnerz.devonian.features.misc.OldSymbols;
import com.github.synnerz.devonian.utils.ObfuscatedBakedGlyph;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GlyphSource;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Font.class)
public class FontMixin {
    @WrapOperation(
        method = "getGlyph",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GlyphSource;getRandomGlyph(Lnet/minecraft/util/RandomSource;I)Lnet/minecraft/client/gui/font/glyphs/BakedGlyph;")
    )
    private BakedGlyph devonian$fixObfuText(GlyphSource instance, RandomSource randomSource, int i, Operation<BakedGlyph> original) {
        BakedGlyph obfu = original.call(instance, randomSource, i);
        if (!FixObfuscatedText.INSTANCE.isEnabled()) return obfu;
        // because shadowing locals is too hard
        BakedGlyph orig = instance.getGlyph(i);
        return new ObfuscatedBakedGlyph(obfu, orig.info());
    }

    @WrapOperation(
        method = "getGlyph",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GlyphSource;getGlyph(I)Lnet/minecraft/client/gui/font/glyphs/BakedGlyph;")
    )
    private BakedGlyph devonian$oldSymbols(GlyphSource instance, int i, Operation<BakedGlyph> original) {
        if (OldSymbols.INSTANCE.isEnabled()) {
            int offset = i - 0xE000;
            if (0 <= offset && offset < OldSymbols.REPLACEMENTS.length) {
                int c = OldSymbols.REPLACEMENTS[offset];
                if (c != 0) i = c;
            }
        }

        return original.call(instance, i);
    }
}
