package com.github.synnerz.devonian.hud.texthud

import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.config.NullableHudData
import com.github.synnerz.devonian.hud.HudFeature
import com.github.synnerz.devonian.hud.texthud.StylizedTextHud.*
import com.github.synnerz.devonian.utils.BoundingBox
import com.github.synnerz.devonian.utils.StringUtils.camelCaseToSentence
import com.github.synnerz.devonian.utils.render.Render2D
import com.github.synnerz.devonian.utils.render.Render2D.height
import com.github.synnerz.devonian.utils.render.Render2D.width
import net.minecraft.client.gui.GuiGraphics
import org.lwjgl.glfw.GLFW
import java.awt.Color

abstract class TextHudFeature(
    configName: String,
    description: String = "",
    category: Categories = Categories.MISC,
    area: String? = null,
    subarea: String? = null,
    searchTags: Set<String> = emptySet(),
    hudConfigName: String = configName[0].uppercase() + configName.substring(1),
    displayName: String = configName.camelCaseToSentence(),
    cheeto: Boolean = false,
    isInternal: Boolean = false,
    subcategory: String = "General",
    isHidden: Boolean = false,
    subcategories: List<String> = emptyList(),
) : HudFeature(
    configName,
    description,
    category,
    area,
    subarea,
    searchTags,
    hudConfigName,
    displayName,
    cheeto,
    isInternal,
    subcategory,
    isHidden,
    subcategories,
), DataProvider, ITextHud {
    abstract fun getEditText(): List<String>

    protected var isEditing = false
    override var anchor = Anchor.NW
    override var align = Align.Left
    override var shadow = Shadow.Drop
    override var backdrop = Backdrop.None

    protected open fun createHud(): StylizedTextHud = StylizedTextHud(configName, this)

    protected val hud by lazy { createHud() }

    override fun load() {
        val data = Config.getHud(legacyName)

        data.x?.let { x = it }
        data.y?.let { y = it }
        data.scale?.let { scale = it }
        data.anchor?.let { anchor = Anchor.from(it) }
        data.align?.let { align = Align.from(it) }
        data.shadow?.let { shadow = Shadow.from(it) }
        data.backdrop?.let { backdrop = Backdrop.from(it) }
    }

    override fun save() {
        Config.setHud(legacyName, NullableHudData.from(this))
    }

    override fun getWidth() = hud.getWidth()
    override fun getLineHeight() = hud.getLineHeight()
    override fun getHeight() = hud.getHeight()
    override fun getBounds(): BoundingBox = hud.getBounds()

    override fun drawImpl(ctx: GuiGraphics) {
        if (isEditing) hud.resetLines()
        isEditing = false
        hud.draw(ctx)
    }

    open fun setEditDisplay() {
        hud.setLines(getEditText())
    }

    override fun sampleDraw(ctx: GuiGraphics, mx: Int, my: Int, selected: Boolean) {
        if (!isEditing) hud.storeLines()
        isEditing = true
        setEditDisplay()
        hud.draw(ctx)

        super.sampleDraw(ctx, mx, my, selected)

        Render2D.drawCircle(ctx, (x + 0.5).toInt(), (y + 0.5).toInt(), 3, Color.RED)
    }

    override fun clearLines() = apply { if (!isEditing) hud.clearLines() }
    override fun addLine(s: String) = apply { if (!isEditing) hud.addLine(s) }
    override fun addLines(s: List<String>) = apply { if (!isEditing) hud.addLines(s) }
    override fun setLine(s: String) = apply { if (!isEditing) hud.setLine(s) }
    override fun setLines(s: List<String>) = apply { if (!isEditing) hud.setLines(s) }
    override fun removeLine(i: Int) = apply { if (!isEditing) hud.removeLine(i) }
    override fun storeLines(): ITextHud = apply { hud.storeLines() }
    override fun resetLines(): ITextHud = apply { hud.resetLines() }

    override fun onKeyPress(keyCode: Int) {
        super.onKeyPress(keyCode)

        when (keyCode) {
            GLFW.GLFW_KEY_1 -> anchor = anchor.cycle()
            GLFW.GLFW_KEY_2 -> align = align.cycle()
            GLFW.GLFW_KEY_3 -> shadow = shadow.cycle()
            GLFW.GLFW_KEY_4 -> backdrop = backdrop.cycle()
        }
    }

    override fun coerceX(v: Double): Double {
        if (!dirty && !isEditing) return super.coerceX(v)

        val w = getEditText().maxOf { it.width() }.toDouble()
        return v.coerceIn(MARGIN .. window.guiScaledWidth - w - MARGIN)
    }

    override fun coerceY(v: Double): Double {
        if (!dirty && !isEditing) return super.coerceY(v)

        val h = getEditText().maxOf { it.height() }.toDouble()
        return v.coerceIn(MARGIN .. window.guiScaledHeight - h - MARGIN)
    }
}