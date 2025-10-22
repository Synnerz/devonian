package com.github.synnerz.devonian.hud

import com.github.synnerz.devonian.utils.JsonUtils
import com.github.synnerz.devonian.utils.Render2D
import com.github.synnerz.devonian.utils.Render2D.height
import com.github.synnerz.devonian.utils.Render2D.width
import net.minecraft.client.gui.DrawContext
import java.awt.Color

// TODO: make this its own class and add TextHud separately
open class Hud(val name: String, val string: String) {
    var x = 10
    var y = 10
    var scale = 1f

    init {
        JsonUtils.afterLoad {
            var currentHud = JsonUtils.getHud(name)
            if (currentHud == null) {
                JsonUtils.setHud(name, x, y, scale)
                currentHud = JsonUtils.getHud(name)
            }

            x = currentHud!!.get("x").asInt
            y = currentHud.get("y").asInt
            scale = currentHud.get("scale").asFloat
        }
    }

    fun inBounds(mx: Double, my: Double): Boolean {
        val width = string.width()
        val height = string.height()

        return mx >= x && mx <= x + width && my >= y && my <= y + height
    }

    fun onMouseScroll(dir: Double) {
        if (dir == 1.0) {
            scale += 0.02f
            return
        }

        scale -= 0.02f
    }

    fun onMouseDrag(dx: Double, dy: Double) {
        x += dx.toInt()
        y += dy.toInt()
    }

    open fun sampleDraw(ctx: DrawContext, mx: Int, my: Int) {
        ctx.drawBorder(
            x - 2,
            y - 2,
            string.width() + 4,
            string.height() + 4,
            if (!inBounds(mx.toDouble(), my.toDouble())) Color.GRAY.rgb
            else Color.WHITE.rgb
        )

        if (string.contains("\n")) {
            Render2D.drawStringNW(
                ctx,
                string,
                x, y, scale
            )
            return
        }

        Render2D.drawString(
            ctx,
            string,
            x, y, scale
        )
    }

    fun save() {
        JsonUtils.setHud(name, x, y, scale)
    }
}