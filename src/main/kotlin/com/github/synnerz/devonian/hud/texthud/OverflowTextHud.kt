package com.github.synnerz.devonian.hud.texthud

import net.minecraft.client.gui.GuiGraphicsExtractor
import java.util.LinkedList
import kotlin.math.pow

class OverflowTextHud(name: String, data: DataProvider) : StylizedTextHud(name, data) {
    private val lineInsertTimes = LinkedList<Long>()

    init {
        lineSpacingInternal = 2f
    }

    var ttl = 5_000L
    var easeIn = 500L
    var easeOut = 500L

    /**
     * getOffset: [0, ttl] -> [-1, 1]
     */
    private fun getOffset(t: Long): Float = when {
        t < easeIn -> -(1f - t / easeIn.toFloat()).pow(3)
        t < ttl - easeOut -> 0f
        else -> ((t - ttl + easeOut) / easeOut.toFloat()).pow(3)
    }

    /**
     * getOpacity: [0, ttl] -> [0, 1]
     */
    private fun getOpacity(t: Long): Float = when {
        t < ttl - easeOut -> 1f
        else -> 1f - ((t - ttl + easeOut) / easeOut.toFloat()).pow(2)
    }

    override fun getHeight(): Double =
        if (lines.isEmpty()) 0.0
        else lines.size * getLineHeight() + renderer.getRenderMetrics().descent * renderScale

    override fun clearLines(): ITextHud {
        lineInsertTimes.clear()
        return super.clearLines()
    }

    override fun addLine(s: String): ITextHud {
        lineInsertTimes.offer(System.currentTimeMillis())
        return super.addLine(s)
    }

    override fun addLines(s: List<String>): ITextHud {
        val t = System.currentTimeMillis()
        repeat(s.size) { lineInsertTimes.offer(t) }
        return super.addLines(s)
    }

    override fun setLine(s: String): ITextHud {
        lineInsertTimes.clear()
        lineInsertTimes.offer(System.currentTimeMillis() - easeIn)
        return super.setLine(s)
    }

    override fun setLines(s: List<String>): ITextHud {
        lineInsertTimes.clear()
        val t = System.currentTimeMillis() - easeIn
        repeat(s.size) { lineInsertTimes.offer(t) }
        return super.setLines(s)
    }

    override fun updateLines() {
        if (lines.isNotEmpty()) {
            val t = System.currentTimeMillis() - ttl
            val iter = lineInsertTimes.iterator()
            while (lines.isNotEmpty()) {
                if (iter.next() >= t) break
                iter.remove()
                lines.removeFirst()
                markImage()
            }
        }

        super.updateLines()
    }

    override fun renderText(ctx: GuiGraphicsExtractor) {
        val iter = lineInsertTimes.iterator()
        val actualX = x
        val t = System.currentTimeMillis()
        for ((i, line) in lines.withIndex()) {
            val lineT = iter.next()
            val xo = getOffset(t - lineT)
            val a = getOpacity(t - lineT)
            val color = 0xFFFFFF or ((a * 255).toInt() shl 24)

            x += xo * getWidth()
            renderer.renderLine(ctx, line.id, i, color)
            x = actualX
        }
    }
}