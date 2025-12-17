package com.github.synnerz.devonian.utils

import com.mojang.blaze3d.font.GlyphInfo
import net.minecraft.client.gui.font.glyphs.BakedGlyph

class ObfuscatedBakedGlyph(private val delegate: BakedGlyph, private val orig: GlyphInfo?) : BakedGlyph by delegate {
    override fun info(): GlyphInfo? {
        return orig
    }
}