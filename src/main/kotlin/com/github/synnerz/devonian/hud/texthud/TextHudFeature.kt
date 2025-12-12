package com.github.synnerz.devonian.hud.texthud

import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.config.NullableHudData
import com.github.synnerz.devonian.hud.HudFeature
import com.github.synnerz.devonian.hud.texthud.StylizedTextHud.*
import com.github.synnerz.devonian.utils.BoundingBox
import com.github.synnerz.devonian.utils.Render2D
import com.github.synnerz.devonian.utils.StringUtils.camelCaseToSentence
import net.minecraft.client.gui.GuiGraphics
import org.lwjgl.glfw.GLFW
import java.awt.Color

abstract class TextHudFeature(
    configName: String,
    description: String = "",
    category: Categories = Categories.MISC,
    area: String? = null,
    subarea: String? = null,
    hudConfigName: String = configName[0].uppercase() + configName.substring(1),
    displayName: String = configName.camelCaseToSentence(),
    cheeto: Boolean = false,
    isInternal: Boolean = false,
    subcategory: String = "General",
) : HudFeature(
    configName,
    description,
    category,
    area,
    subarea,
    hudConfigName,
    displayName,
    cheeto,
    isInternal,
    subcategory
), DataProvider, ITextHud {
    abstract fun getEditText(): List<String>

    protected var isEditing = false
    override var anchor = Anchor.NW
    override var align = Align.Left
    override var shadow = true
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
        data.shadow?.let { shadow = it }
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
        isEditing = false
        hud.draw(ctx)
    }

    open fun setEditDisplay() {
        hud.setLines(getEditText())
    }

    override fun sampleDraw(ctx: GuiGraphics, mx: Int, my: Int, selected: Boolean) {
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

    override fun onKeyPress(keyCode: Int) {
        super.onKeyPress(keyCode)

        when (keyCode) {
            GLFW.GLFW_KEY_1 -> anchor = anchor.cycle()
            GLFW.GLFW_KEY_2 -> align = align.cycle()
            GLFW.GLFW_KEY_3 -> shadow = !shadow
            GLFW.GLFW_KEY_4 -> backdrop = backdrop.cycle()
        }
    }
}