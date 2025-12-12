package com.github.synnerz.devonian.hud

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.config.NullableHudData
import com.github.synnerz.devonian.config.ui.talium.ConfigGui
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.features.HudManagerHider
import com.github.synnerz.devonian.utils.BoundingBox
import com.github.synnerz.devonian.utils.Render2D
import com.github.synnerz.devonian.utils.Render2D.height
import com.github.synnerz.devonian.utils.Render2D.width
import com.github.synnerz.devonian.utils.StringUtils.camelCaseToSentence
import net.minecraft.client.gui.GuiGraphics
import org.lwjgl.glfw.GLFW
import java.awt.Color
import kotlin.math.ceil
import kotlin.math.sign
import kotlin.math.withSign

abstract class HudFeature(
    configName: String,
    description: String,
    category: Categories = Categories.MISC,
    area: String? = null,
    subarea: String? = null,
    protected val legacyName: String = configName[0].uppercase() + configName.substring(1),
    displayName: String = configName.camelCaseToSentence(),
    cheeto: Boolean = false,
    isInternal: Boolean = false,
    subcategory: String = "General",
) : Feature(configName, description, category, area, subarea, displayName, cheeto, isInternal, subcategory) {
    var x = 10.0
    var y = 10.0
    var scale = 1f

    init {
        HudManager.addHud(this)
    }

    open fun load() {
        val data = Config.getHud(legacyName)

        data.x?.let { x = it }
        data.y?.let { y = it }
        data.scale?.let { scale = it }
    }

    open fun save() {
        Config.setHud(legacyName, NullableHudData(x, y, scale))
    }

    abstract fun getBounds(): BoundingBox

    fun inBounds(mx: Double, my: Double): Boolean = getBounds().inBounds(mx, my)

    open fun onMouseScroll(dir: Double) {
        val INCREMENT = 0.02f
        val delta = INCREMENT.withSign(dir.sign.toFloat())

        scale = (scale + delta).coerceAtLeast(0.1f)
    }

    open fun onMouseDrag(dx: Double, dy: Double) {
        val MARGIN = 2.0
        val window = Devonian.minecraft.window
        x = (x + dx).coerceIn(MARGIN .. window.guiScaledWidth - MARGIN)
        y = (y + dy).coerceIn(MARGIN .. window.guiScaledHeight - MARGIN)
    }

    open fun onMouseClick(mx: Double, my: Double, mbtn: Int) {
        when (mbtn) {
            GLFW.GLFW_MOUSE_BUTTON_RIGHT -> toggle()
            GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> {
                if (!isInternal) ConfigGui.scrollToConfig(configSwitch)
            }
        }
    }

    open fun onKeyPress(keyCode: Int) {
        val INCREMENT = 5.0
        val MARGIN = 2.0
        var dx = 0.0
        var dy = 0.0
        when (keyCode) {
            GLFW.GLFW_KEY_LEFT -> dx = -INCREMENT
            GLFW.GLFW_KEY_RIGHT -> dx = INCREMENT
            GLFW.GLFW_KEY_UP -> dy = -INCREMENT
            GLFW.GLFW_KEY_DOWN -> dy = INCREMENT
            GLFW.GLFW_KEY_MINUS -> return onMouseScroll(-1.0)
            GLFW.GLFW_KEY_EQUAL -> return onMouseScroll(+1.0)
        }
        val window = Devonian.minecraft.window
        x = (x + dx).coerceIn(MARGIN .. window.guiScaledWidth - MARGIN)
        y = (y + dy).coerceIn(MARGIN .. window.guiScaledHeight - MARGIN)
    }

    abstract fun drawImpl(ctx: GuiGraphics)

    open fun draw(ctx: GuiGraphics) {
        if (HudManager.isEditing) return

        drawImpl(ctx)
    }

    open fun sampleDraw(ctx: GuiGraphics, mx: Int, my: Int, selected: Boolean) {
        val bounds = getBounds()

        if (!isInternal && !isEnabled()) {
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
                textBounds.second.x.toInt(),
                textBounds.second.y.toInt(),
                textBounds.first.toFloat()
            )
        }

        ctx.submitOutline(
            bounds.x.toInt(),
            bounds.y.toInt(),
            ceil(bounds.w).toInt(),
            ceil(bounds.h).toInt(),
            if (selected) Color.WHITE.rgb
            else Color.GRAY.rgb
        )
    }

    fun isVisibleEdit() = isEnabled() || isInternal || !HudManagerHider.isEnabled()
}