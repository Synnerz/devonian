package com.github.synnerz.devonian.hud

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.BoundingBox
import com.github.synnerz.devonian.utils.JsonUtils
import com.github.synnerz.devonian.utils.Render2D
import com.github.synnerz.devonian.utils.Render2D.height
import com.github.synnerz.devonian.utils.Render2D.width
import net.minecraft.client.gui.DrawContext
import org.lwjgl.glfw.GLFW
import java.awt.Color
import kotlin.math.ceil
import kotlin.math.withSign

abstract class HudFeature(
    configName: String,
    area: String? = null,
    subarea: String? = null,
    protected val legacyName: String = configName[0].uppercase() + configName.substring(1)
) : Feature(configName, area, subarea) {
    var x = 10.0
    var y = 10.0
    var scale = 1f

    init {
        HudManager.addHud(this)
    }

    open fun _hudInit() {
        JsonUtils.afterLoad {
            var currentHud = JsonUtils.getHud(legacyName)
            if (currentHud == null) {
                JsonUtils.setHud(legacyName, x, y, scale)
                currentHud = JsonUtils.getHud(legacyName)
            }

            x = currentHud!!.get("x")?.asDouble ?: 10.0
            y = currentHud.get("y")?.asDouble ?: 10.0
            scale = currentHud.get("scale")?.asFloat ?: 1f
        }
    }

    abstract fun getBounds(): BoundingBox

    fun inBounds(mx: Double, my: Double): Boolean = getBounds().inBounds(mx, my)

    open fun onMouseScroll(dir: Double) {
        val INCREMENT = 0.02f
        val delta = INCREMENT.withSign(if (dir == 1.0) 1 else -1)

        scale = (scale + delta).coerceAtLeast(0.1f)
    }

    open fun onMouseDrag(dx: Double, dy: Double) {
        val MARGIN = 10
        val bounds = getBounds()
        val window = Devonian.minecraft.window
        x = (x + dx).coerceIn(-bounds.w + MARGIN..window.width / window.scaleFactor - MARGIN)
        y = (y + dy).coerceIn(-bounds.h + MARGIN..window.height / window.scaleFactor - MARGIN)
    }

    open fun onMouseClick(mx: Double, my: Double, mbtn: Int) {
        if (mbtn == 1) toggle()
    }

    open fun onKeyPress(keyCode: Int) {
        val INCREMENT = 5
        val MARGIN = 10
        var dx = 0
        var dy = 0
        when (keyCode) {
            GLFW.GLFW_KEY_LEFT -> dx = -INCREMENT
            GLFW.GLFW_KEY_RIGHT -> dx = INCREMENT
            GLFW.GLFW_KEY_UP -> dy = -INCREMENT
            GLFW.GLFW_KEY_DOWN -> dy = INCREMENT
        }
        val bounds = getBounds()
        val window = Devonian.minecraft.window
        x = (x + dx).coerceIn(-bounds.w + MARGIN..window.width / window.scaleFactor - MARGIN)
        y = (y + dy).coerceIn(-bounds.h + MARGIN..window.height / window.scaleFactor - MARGIN)
    }

    abstract fun drawImpl(ctx: DrawContext)

    open fun draw(ctx: DrawContext) {
        if (HudManager.isEditing) return

        drawImpl(ctx)
    }

    open fun sampleDraw(ctx: DrawContext, mx: Int, my: Int, selected: Boolean) {
        val bounds = getBounds()

        if (!isEnabled()) {
            ctx.fill(
                bounds.x.toInt(),
                bounds.y.toInt(),
                (bounds.x + bounds.w).toInt(),
                (bounds.y + bounds.h).toInt(),
                Color.RED.let { Color(it.red, it.green, it.blue, 80) }.rgb
            )
            val msg = "&4Disabled"
            val msgBounds = BoundingBox(0.0, 0.0, msg.width().toDouble(), msg.height().toDouble())
            val textBounds = msgBounds.fitInside(
                BoundingBox(
                    bounds.x + bounds.w * 0.125,
                    bounds.y + bounds.h * 0.125,
                    bounds.w * 0.75,
                    bounds.h * 0.75
                )
            )
            Render2D.drawString(
                ctx,
                msg,
                textBounds.x.toInt(),
                textBounds.y.toInt(),
                textBounds.w.toFloat() / msgBounds.w.toFloat()
            )
        }

        ctx.drawBorder(
            bounds.x.toInt(),
            bounds.y.toInt(),
            ceil(bounds.w).toInt(),
            ceil(bounds.h).toInt(),
            if (selected) Color.WHITE.rgb
            else Color.GRAY.rgb
        )
    }

    open fun save() {
        JsonUtils.setHud(legacyName, x, y, scale)
    }
}