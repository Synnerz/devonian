package com.github.synnerz.devonian.utils.render

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.utils.StringUtils.clearCodes
import net.minecraft.client.gui.DrawContext
import net.minecraft.util.Formatting
import kotlin.math.max

object Render2D {
    private val formattingRegex = "(?<!\\\\\\\\)&(?=[0-9a-fk-or])".toRegex()
    val textRenderer = Devonian.minecraft.textRenderer
    val window get() = Devonian.minecraft.window
    val mouse = Devonian.minecraft.mouse
    val screenWidth get() = window.width
    val screenHeight get() = window.height
    val scaledWidth get() = window.scaledWidth
    val scaledHeight get() = window.scaledHeight

    @JvmOverloads
    fun drawString(ctx: DrawContext, str: String, x: Int, y: Int, scale: Float = 1f, shadow: Boolean = true) {
        val matrices = ctx.matrices
        if (scale != 1f) {
            matrices.push()
            matrices.scale(scale, scale, 1f)
        }

        ctx.drawText(
            textRenderer,
            str.replace(formattingRegex, "${Formatting.FORMATTING_CODE_PREFIX}"),
            x,
            y,
            -1,
            shadow
        )

        if (scale != 1f) matrices.pop()
    }

    @JvmOverloads
    fun drawStringNW(ctx: DrawContext, str: String, x: Int, y: Int, scale: Float = 1f, shadow: Boolean = true) {
        var yy = y
        str.split("\n").forEach {
            drawString(ctx, it, x, yy, scale, shadow)
            yy += 10
        }
    }

    fun String.width(): Int {
        val newlines = this.split("\n")
        if (newlines.size <= 1) return textRenderer.getWidth(this.clearCodes())

        var maxWidth = 0

        for (line in newlines)
            maxWidth = max(maxWidth, textRenderer.getWidth(line.clearCodes()))

        return maxWidth
    }

    fun String.height(): Int {
        val newlines = this.split("\n")
        if (newlines.size <= 1) return textRenderer.fontHeight

        return textRenderer.fontHeight * (newlines.size + 1)
    }

    object Mouse {
        val x get() = mouse.x * scaledWidth / max(1, screenWidth)
        val y get() = mouse.y * scaledHeight / max(1, screenHeight)
    }
}