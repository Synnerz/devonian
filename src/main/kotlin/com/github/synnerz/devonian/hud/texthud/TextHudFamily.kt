package com.github.synnerz.devonian.hud.texthud

import net.minecraft.client.gui.GuiGraphics

class TextHudFamily(name: String, data: DataProvider) : TextHud(name, data) {
    val children = mutableListOf<TextHud>()

    fun createChildProvider() = DerivedProvider(this)

    override fun getWidth(): Double = children.sumOf { it.getWidth() }

    override fun getLineHeight(): Double = children.maxOf { it.getLineHeight() }

    override fun getHeight(): Double = children.maxOf { it.getHeight() }

    override fun clearLines() = throw UnsupportedOperationException("operate on children directly")
    override fun addLine(s: String) = throw UnsupportedOperationException("operate on children directly")
    override fun addLines(s: List<String>) = throw UnsupportedOperationException("operate on children directly")
    override fun setLine(s: String) = throw UnsupportedOperationException("operate on children directly")
    override fun setLines(s: List<String>) = throw UnsupportedOperationException("operate on children directly")
    override fun removeLine(i: Int) = throw UnsupportedOperationException("operate on children directly")

    override fun draw(ctx: GuiGraphics) {
        val bounds = getBounds()
        var x = 0.0

        children.forEach { child ->
            child.x = bounds.x + x
            child.y = bounds.y + bounds.h - child.getHeight()
            x += child.getWidth()
            child.draw(ctx)
        }
    }
}

class DerivedProvider(
    val parent: TextHud,
) : DataProvider {
    override var x: Double = 0.0
    override var y: Double = 0.0

    override var scale: Float
        get() = parent.scale
        set(_) = throw UnsupportedOperationException()

    override var anchor: TextHud.Anchor
        get() = TextHud.Anchor.NW
        set(_) = throw UnsupportedOperationException()

    override var align: TextHud.Align
        get() = parent.align
        set(_) = throw UnsupportedOperationException()

    override var shadow: Boolean
        get() = parent.shadow
        set(_) = throw UnsupportedOperationException()

    override var backdrop: TextHud.Backdrop
        get() = parent.backdrop
        set(_) = throw UnsupportedOperationException()
}