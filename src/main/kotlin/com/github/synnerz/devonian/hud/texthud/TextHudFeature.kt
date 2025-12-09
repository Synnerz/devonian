package com.github.synnerz.devonian.hud.texthud

import com.github.synnerz.devonian.hud.HudFeature
import com.github.synnerz.devonian.hud.texthud.TextHud.*
import com.github.synnerz.devonian.utils.BoundingBox
import com.github.synnerz.devonian.config.JsonUtils
import com.github.synnerz.devonian.utils.Render2D
import net.minecraft.client.gui.GuiGraphics
import org.lwjgl.glfw.GLFW
import java.awt.Color

abstract class TextHudFeature(
    configName: String,
    description: String = "",
    category: String = "Misc",
    area: String? = null,
    subarea: String? = null,
    hudConfigName: String = configName[0].uppercase() + configName.substring(1),
) : HudFeature(configName, description, category, area, subarea, hudConfigName), DataProvider, ITextHud {
    abstract fun getEditText(): List<String>

    protected var isEditing = false
    override var anchor = Anchor.NW
    override var align = Align.Left
    override var shadow = true
    override var backdrop = Backdrop.None

    protected open fun createHud() = TextHud(configName, this)

    protected val hud = createHud()

    override fun _hudInit() {
        JsonUtils.afterLoad {
            var currentHud = JsonUtils.getHud(legacyName)
            if (currentHud == null) {
                JsonUtils.setHud(legacyName, x, y, scale, anchor.ordinal, align.ordinal, shadow, backdrop.ordinal)
                currentHud = JsonUtils.getHud(legacyName)
            }

            x = currentHud!!.get("x")?.asDouble ?: 10.0
            y = currentHud.get("y")?.asDouble ?: 10.0
            scale = currentHud.get("scale")?.asFloat ?: 1f
            anchor = Anchor.from(currentHud.get("anchor")?.asInt ?: 0)
            align = Align.from(currentHud.get("align")?.asInt ?: 0)
            shadow = currentHud.get("shadow")?.asBoolean ?: true
            backdrop = Backdrop.from(currentHud.get("backdrop")?.asInt ?: 0)
        }
    }

    override fun save() {
        JsonUtils.setHud(legacyName, x, y, scale, anchor.ordinal, align.ordinal, shadow, backdrop.ordinal)
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