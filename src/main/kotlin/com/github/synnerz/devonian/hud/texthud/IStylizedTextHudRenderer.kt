package com.github.synnerz.devonian.hud.texthud

import net.minecraft.client.gui.GuiGraphics

abstract class IStylizedTextHudRenderer(val name: String) {
    lateinit var parent: StylizedTextHud
    open fun onFontUpdate() {}
    open fun onUpdate() {}
    abstract fun onUpdateLine(str: String, params: StylizedTextHud.TextRenderParams): StylizedTextHud.LineData
    open fun updateCleanup() {}
    open fun onUpdateImage() {}
    abstract fun renderText(ctx: GuiGraphics)
    open fun dispose() {}
}