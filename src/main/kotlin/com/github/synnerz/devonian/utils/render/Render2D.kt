package com.github.synnerz.devonian.utils.render

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.features.misc.inventory.InventoryScale
import com.github.synnerz.devonian.utils.StringUtils.clearCodes
import com.github.synnerz.devonian.utils.render.states.QuadRenderState
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import org.joml.Matrix3x2f
import java.awt.Color
import kotlin.math.hypot
import kotlin.math.max

object Render2D {
    private val formattingRegex = "(?<!\\\\\\\\)&(?=[0-9a-fk-or])".toRegex()
    val textRenderer = Devonian.minecraft.font
    val window get() = Devonian.minecraft.window
    val mouse = Devonian.minecraft.mouseHandler
    val scale get() =
        if (InventoryScale.isEnabled() && Devonian.minecraft.screen != null)
            InventoryScale.getScale()
        else
            Devonian.minecraft.window.guiScale
    val screenWidth get() = window.width
    val screenHeight get() = window.height
    val scaledWidth get() = window.guiScaledWidth
    val scaledHeight get() = window.guiScaledHeight
    val customScaleWidth get() = screenWidth / scale
    val customScaleHeight get() = screenHeight / scale

    @JvmOverloads
    fun drawString(ctx: GuiGraphicsExtractor, str: String, x: Int, y: Int, scale: Float = 1f, shadow: Boolean = true) {
        val matrices = ctx.pose()
        matrices.pushMatrix()
        matrices.translate(x.toFloat(), y.toFloat())
        if (scale != 1f) matrices.scale(scale, scale)

        ctx.text(
            textRenderer,
            str.replace(formattingRegex, "${ChatFormatting.PREFIX_CODE}"),
            0,
            0,
            -1,
            shadow
        )

        matrices.popMatrix()
    }

    @JvmOverloads
    fun drawStringNW(ctx: GuiGraphicsExtractor, str: String, x: Int, y: Int, scale: Float = 1f, shadow: Boolean = true) {
        var yy = y
        str.split("\n").forEach {
            drawString(ctx, it, x, yy, scale, shadow)
            yy += 10
        }
    }

    @JvmOverloads
    fun drawRect(ctx: GuiGraphicsExtractor, x: Int, y: Int, width: Int, height: Int, color: Color = Color.WHITE) {
        ctx.fill(RenderPipelines.GUI, x, y, x + width, y + height, color.rgb)
    }

    fun drawWireRect(ctx: GuiGraphicsExtractor, x: Int, y: Int, w: Int, h: Int, c: Color, lw: Int = 1) {
        val rgb = c.rgb
        ctx.fill(x, y, x + w, y + lw, rgb)
        ctx.fill(x, y + lw, x + lw, y + h, rgb)
        ctx.fill(x + w - lw, y + lw, x + w, y + h, rgb)
        ctx.fill(x + lw, y + h - lw, x + w - lw, y + h, rgb)
    }

    fun drawCircle(ctx: GuiGraphicsExtractor, cx: Int, cy: Int, radius: Int, color: Color = Color.WHITE) {
        var x = 0
        var y = radius
        var d = 3 - 2 * radius

        while (x <= y) {
            ctx.horizontalLine(cx - x, cx + x, cy + y, color.rgb)
            ctx.horizontalLine(cx - x, cx + x, cy - y, color.rgb)
            ctx.horizontalLine(cx - y, cx + y, cy + x, color.rgb)
            ctx.horizontalLine(cx - y, cx + y, cy - x, color.rgb)

            if (d < 0) {
                d += 4 * x + 6
            } else {
                d += 4 * (x - y) + 10
                y--
            }
            x++
        }
    }

    fun drawLine(
        ctx: GuiGraphicsExtractor,
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        c: Color,
        lw: Float = 1f,
    ) {
        val mat = Matrix3x2f(ctx.pose())
        var dx = y1 - y2
        var dy = x2 - x1
        val f = lw / hypot(dx, dy)
        dx *= f
        dy *= f
        ctx.guiRenderState.addGuiElement(
            QuadRenderState(
                RenderPipelines.GUI,
                mat,
                x1 + dx, y1 + dy,
                x1 - dx, y1 - dy,
                x2 + dx, y2 + dy,
                x2 - dx, y2 - dy,
                c.rgb,
                ctx.scissorStack.peek(),
            )
        )
    }

    fun String.width(): Int {
        val newlines = this.split("\n")
        if (newlines.size <= 1) return textRenderer.width(this.clearCodes())

        var maxWidth = 0

        for (line in newlines)
            maxWidth = max(maxWidth, textRenderer.width(line.clearCodes()))

        return maxWidth
    }

    fun String.height(): Int {
        val newlines = this.split("\n")
        if (newlines.size <= 1) return textRenderer.lineHeight

        return textRenderer.lineHeight * (newlines.size + 1)
    }

    object Mouse {
        val x get() = mouse.xpos() * customScaleWidth / max(1, screenWidth)
        val y get() = mouse.ypos() * customScaleHeight / max(1, screenHeight)
    }
}