package com.github.synnerz.devonian.hud

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.config.NullableHudData
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.features.HudManagerGrid
import com.github.synnerz.devonian.features.HudManagerHider
import com.github.synnerz.devonian.utils.BoundingBox
import com.github.synnerz.devonian.utils.StringUtils.camelCaseToSentence
import com.github.synnerz.devonian.utils.render.Render2D
import com.github.synnerz.devonian.utils.render.Render2D.height
import com.github.synnerz.devonian.utils.render.Render2D.width
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.lwjgl.glfw.GLFW
import java.awt.Color
import kotlin.math.*

abstract class HudFeature(
    configName: String,
    description: String,
    category: Categories = Categories.MISC,
    area: String? = null,
    subarea: String? = null,
    searchTags: Set<String> = emptySet(),
    protected val legacyName: String = configName[0].uppercase() + configName.substring(1),
    displayName: String = configName.camelCaseToSentence(),
    cheeto: Boolean = false,
    isInternal: Boolean = false,
    subcategory: String = "General",
    isHidden: Boolean = false,
    subcategories: List<String> = emptyList(),
) : Feature(
    configName,
    description,
    category,
    area,
    subarea,
    searchTags,
    displayName,
    cheeto,
    isInternal,
    subcategory,
    isHidden,
    subcategories,
) {
    val window get() = minecraft.window
    val MARGIN = 2.0
    open var dirty = true
    var x = 10.0
    var y = 10.0
    var scale = 1f
    var fromConfig = false

    init {
        HudManager.addHud(this)

        EventBus.once<RenderOverlayEvent> {
            if (!fromConfig) setDefaultValues()
        }
    }

    open fun load() {
        val data = Config.getHud(legacyName)

        data.x?.let { fromConfig = true; x = it }
        data.y?.let { fromConfig = true; y = it }
        data.scale?.let { fromConfig = true; scale = it }
    }

    open fun save() {
        Config.setHud(legacyName, NullableHudData(x, y, scale))
    }

    /**
     * IDEA:
     * since it is possible to import someone else's config and these elements being out of bounds (out of reach)
     * to the user they will think the feature hud simply does not exist, so we need to prevent that by
     * forcing the position to always be coerced between the screen bounds, however this cannot be done as soon
     * as it loads because the "getWindow()" is null at that time, so we delegate it to be in the next
     * rendering frame (whenever that may happen)
    */
    open fun onDirty() {
        x = coerceX(x)
        y = coerceY(y)
        dirty = false
    }

    abstract fun getBounds(): BoundingBox

    fun inBounds(mx: Double, my: Double): Boolean = getBounds().inBounds(mx, my)

    open fun onMouseScroll(dir: Double) {
        val INCREMENT = 0.02f
        val delta = INCREMENT.withSign(dir.sign.toFloat())

        scale = (scale + delta).coerceAtLeast(0.1f)
    }

    open fun onMouseDrag(dx: Double, dy: Double) {
        if (HudManagerGrid.isEnabled()) {
            x = coerceX(snapGrid(HudManager.startDragX + HudManager.cumDragX))
            y = coerceY(snapGrid(HudManager.startDragY + HudManager.cumDragY))
        } else {
            x = coerceX(x + dx)
            y = coerceY(y + dy)
        }
    }

    open fun onMouseClick(mx: Double, my: Double, mbtn: Int) {
        when (mbtn) {
            GLFW.GLFW_MOUSE_BUTTON_RIGHT -> toggle()
        }
    }

    open fun onKeyPress(keyCode: Int) {
        val INCREMENT = 5.0
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

        x = coerceX(x + dx)
        y = coerceY(y + dy)
    }

    abstract fun drawImpl(ctx: GuiGraphicsExtractor)

    open fun draw(ctx: GuiGraphicsExtractor) {
        if (HudManager.isEditing) return

        drawImpl(ctx)
    }

    open fun sampleDraw(ctx: GuiGraphicsExtractor, mx: Int, my: Int, selected: Boolean) {
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

        ctx.outline(
            bounds.x.toInt(),
            bounds.y.toInt(),
            ceil(bounds.w).toInt(),
            ceil(bounds.h).toInt(),
            if (selected) Color.WHITE.rgb
            else Color.GRAY.rgb
        )

        if (dirty) onDirty()
    }

    fun isVisibleEdit() =
        (isEnabled() || isInternal || !HudManagerHider.isEnabled()) &&
        (!isHidden || Devonian.isDev)

    fun snapGrid(v: Double): Double = HudManager.gridSize * (v / HudManager.gridSize).roundToInt()

    open fun coerceX(v: Double): Double {
        return max(MARGIN, min(window.guiScaledWidth - MARGIN, v))
    }

    open fun coerceY(v: Double): Double {
        return max(MARGIN, min(window.guiScaledHeight - MARGIN, v))
    }

    open fun setDefaultValues() {
        x = 10.0
        y = 10.0
        scale = 1f
    }
}