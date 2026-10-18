package com.github.synnerz.devonian.hud.texthud

import com.github.synnerz.devonian.utils.BoundingBox
import net.minecraft.client.gui.GuiGraphicsExtractor

interface ITextHud {
    fun getWidth(): Double
    fun getLineHeight(): Double
    fun getHeight(): Double
    fun getBounds(): BoundingBox
    fun draw(ctx: GuiGraphicsExtractor)
    fun clearLines(): ITextHud
    fun addLine(s: String): ITextHud
    fun addLines(s: List<String>): ITextHud
    fun setLine(s: String): ITextHud
    fun setLines(s: List<String>): ITextHud
    fun storeLines(): ITextHud
    fun resetLines(): ITextHud
    fun invalidateLines(): ITextHud
}