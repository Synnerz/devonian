package com.github.synnerz.devonian.hud.texthud

abstract class FixedWidthTextHud(name: String, data: DataProvider) : StylizedTextHud(name, data) {
    private var maxLine = Line("")

    abstract fun getMaxLine(): String

    override fun markText() {
        super.markText()
        maxLine.dirty = true
    }

    override fun updateLines() {
        super.updateLines()

        val str = getMaxLine()
        if (str != maxLine.str) maxLine = Line(str)

        if (maxLine.dirty) {
            maxLine.data = renderer.onUpdateLine(maxLine.str, lastRenderParams)
            if (maxLine.data!!.width != 0f) maxLine.data!!.width += shadow.getSizeIncrease(fontSize)
            maxLine.dirty = false
            markImage()
        }
        lineWidth = maxLine.data!!.width
    }
}