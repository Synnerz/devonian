package com.github.synnerz.devonian.hud.texthud

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.navigation.ScreenRectangle
import kotlin.math.ceil
import kotlin.math.max

class Marquee(name: String, data: DataProvider) : TextHud(name, data) {
    override fun getWidth(): Double = maxLen * scale

    private var actualX = 0.0

    var maxLen = 100.0
    var scrollSpeed = 20.0
    var freezeTime = 1500.0
    var alternate = false

    override fun draw(ctx: GuiGraphics) {
        val t = System.nanoTime() * 1.0e-6

        actualX = x

        val scrollLength = max(super.getWidth() / scale - maxLen, 0.0)
        val pos = if (scrollSpeed == 0.0 || scrollLength == 0.0) 0.0
        else {
            val scrollTime = scrollLength * 1000.0 / scrollSpeed
            val cycleTime = freezeTime + scrollTime + freezeTime + (if (alternate) scrollTime else 0.0)
            val cycleOffset = t % cycleTime

            if (cycleOffset < freezeTime) 0.0
            else if (cycleOffset < freezeTime + scrollTime) (cycleOffset - freezeTime) / scrollTime
            else if (cycleOffset < freezeTime + scrollTime + freezeTime) 1.0
            else 1.0 - (cycleOffset - freezeTime - scrollTime - freezeTime) / scrollTime
        }

        x -= pos * scrollLength * scale

        super.draw(ctx)
        x = actualX
    }

    override fun drawCachedImage(ctx: GuiGraphics) {
        ctx.scissorStack.push(
            ScreenRectangle(
                ceil(actualX).toInt(), ceil(y).toInt(),
                (maxLen * scale).toInt(), getHeight().toInt()
            )
        )
        super.drawCachedImage(ctx)
        ctx.scissorStack.pop()
    }
}