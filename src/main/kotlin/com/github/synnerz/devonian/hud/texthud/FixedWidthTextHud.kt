package com.github.synnerz.devonian.hud.texthud

class FixedWidthTextHud(maxLine: String, name: String, data: DataProvider) : StylizedTextHud(name, data) {
    private val maxLine = Line(maxLine)

    override fun markText() {
        super.markText()
        maxLine.dirty = true
    }

    override fun updateLines() {
        super.updateLines()

        if (maxLine.dirty) {
            maxLine.data = renderer.onUpdateLine(maxLine.str, lastRenderParams)
            if (maxLine.data!!.width != 0f) maxLine.data!!.width += shadow.getSizeIncrease(fontSize)
            maxLine.dirty = false
        }
        lineWidth = maxLine.data!!.width
    }
}