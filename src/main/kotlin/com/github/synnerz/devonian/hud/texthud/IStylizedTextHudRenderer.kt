package com.github.synnerz.devonian.hud.texthud

import net.minecraft.client.gui.GuiGraphicsExtractor

abstract class IStylizedTextHudRenderer(val name: String) {
    lateinit var parent: StylizedTextHud
    open fun onFontUpdate(fontSize: Float) {}
    open fun onUpdate() {}
    abstract fun onUpdateLine(str: String, params: StylizedTextHud.TextRenderParams): StylizedTextHud.LineData
    open fun updateCleanup() {}
    open fun onUpdateImage(params: StylizedTextHud.TextRenderParams, metrics: StylizedTextHud.TextRenderMetrics) {}
    abstract fun getRenderMetrics(): StylizedTextHud.TextRenderMetrics
    abstract fun renderText(ctx: GuiGraphicsExtractor)
    abstract fun renderLine(ctx: GuiGraphicsExtractor, line: Int, color: Int)
    open fun dispose() {}
}