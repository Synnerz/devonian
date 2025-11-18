package com.github.synnerz.devonian.utils

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.utils.StringUtils.clearCodes
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.RenderType
import java.awt.Color
import kotlin.math.max

object Render2D {
    private val formattingRegex = "(?<!\\\\\\\\)&(?=[0-9a-fk-or])".toRegex()
    val textRenderer = Devonian.minecraft.font
    val window get() = Devonian.minecraft.window
    val mouse = Devonian.minecraft.mouseHandler
    val screenWidth get() = window.width
    val screenHeight get() = window.height
    val scaledWidth get() = window.guiScaledWidth
    val scaledHeight get() = window.guiScaledHeight

    @JvmOverloads
    fun drawString(ctx: GuiGraphics, str: String, x: Int, y: Int, scale: Float = 1f, shadow: Boolean = true) {
        val matrices = ctx.pose()
        matrices.pushPose()
        matrices.translate(x.toFloat(), y.toFloat(), 0f)
        if (scale != 1f) matrices.scale(scale, scale, 1f)

        ctx.drawString(
            textRenderer,
            str.replace(formattingRegex, "${ChatFormatting.PREFIX_CODE}"),
            0,
            0,
            -1,
            shadow
        )

        matrices.popPose()
    }

    @JvmOverloads
    fun drawStringNW(ctx: GuiGraphics, str: String, x: Int, y: Int, scale: Float = 1f, shadow: Boolean = true) {
        var yy = y
        str.split("\n").forEach {
            drawString(ctx, it, x, yy, scale, shadow)
            yy += 10
        }
    }

    @JvmOverloads
    fun drawRect(ctx: GuiGraphics, x: Int, y: Int, width: Int, height: Int, color: Color = Color.WHITE) {
        ctx.fill(RenderType.gui(), x, y, x + width, y + height, color.rgb)
    }

    fun drawCircle(ctx: GuiGraphics, cx: Int, cy: Int, radius: Int, color: Color = Color.WHITE) {
        var x = 0
        var y = radius
        var d = 3 - 2 * radius

        while (x <= y) {
            ctx.hLine(cx - x, cx + x, cy + y, color.rgb)
            ctx.hLine(cx - x, cx + x, cy - y, color.rgb)
            ctx.hLine(cx - y, cx + y, cy + x, color.rgb)
            ctx.hLine(cx - y, cx + y, cy - x, color.rgb)

            if (d < 0) {
                d += 4 * x + 6
            } else {
                d += 4 * (x - y) + 10
                y--
            }
            x++
        }
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
        val x get() = mouse.xpos() * scaledWidth / max(1, screenWidth)
        val y get() = mouse.ypos() * scaledHeight / max(1, screenHeight)
    }
}