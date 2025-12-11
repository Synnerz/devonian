package com.github.synnerz.devonian.hud.texthud

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.hud.texthud.StylizedTextHud.Align
import com.github.synnerz.devonian.hud.texthud.StylizedTextHud.Backdrop
import com.github.synnerz.devonian.hud.texthud.StylizedTextHud.Companion.BASE_FONT_SIZE
import com.github.synnerz.devonian.utils.QuadRenderState
import com.github.synnerz.devonian.utils.StringUtils.replaceCodes
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.render.state.GuiTextRenderState
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import org.joml.Matrix3x2f

class MCTextHudRenderer(name: String) : IStylizedTextHudRenderer(name) {
    override fun onUpdateLine(
        str: String,
        params: StylizedTextHud.TextRenderParams
    ): StylizedTextHud.LineData {
        val font = Devonian.minecraft.font
        val comp = Component.literal(str.replaceCodes())
        val w = (font?.width(comp) ?: 0) * parent.scale / parent.renderScale.toFloat()
        return CompLineData(
            w, w,
            parent.fontSize,
            parent.fontSize * 0.25f,
            false,
            comp,
        )
    }

    override fun renderText(ctx: GuiGraphics) {
        val font = Devonian.minecraft.font ?: return
        val bounds = parent.getBounds()
        ctx.pose()
            .pushMatrix()
            .translate(bounds.x.toFloat(), bounds.y.toFloat())
            .scale(parent.scale)
        val mat = Matrix3x2f(ctx.pose())

        if (parent.backdrop == Backdrop.Full) ctx.guiRenderState.submitGuiElement(
            QuadRenderState(
                RenderPipelines.GUI,
                mat,
                0f, 0f,
                bounds.w.toFloat() / parent.scale, bounds.h.toFloat() / parent.scale,
                0x40000000,
                ctx.scissorStack.peek()
            )
        )

        parent.lines.forEachIndexed { i, line ->
            val data = line.data as CompLineData

            val y = i * BASE_FONT_SIZE
            val x = when (parent.align) {
                Align.Left -> 0f
                Align.Right -> parent.lineVisualWidth - data.visualWidth
                Align.Center -> (parent.lineVisualWidth - data.visualWidth) * 0.5f
            } / parent.scale * parent.renderScale.toFloat()

            if (parent.backdrop == Backdrop.Line) ctx.guiRenderState.submitGuiElement(
                QuadRenderState(
                    RenderPipelines.GUI,
                    mat,
                    x, y,
                    (x + data.visualWidth / parent.scale * parent.renderScale).toFloat(),
                    (y + (data.ascent + data.descent) / parent.scale * parent.renderScale).toFloat(),
                    0x40000000,
                    ctx.scissorStack.peek()
                )
            )

            ctx.guiRenderState.submitText(
                GuiTextRenderState(
                    font,
                    data.comp.visualOrderText,
                    mat,
                    x.toInt(), y.toInt() + 2,
                    -1,
                    // if (parent.backdrop == Backdrop.Line) 0x40000000 else 0,
                    0,
                    parent.shadow,
                    ctx.scissorStack.peek()
                )
            )
        }

        ctx.pose().popMatrix()
    }

    class CompLineData(
        width: Float,
        visualWidth: Float,
        ascent: Float,
        descent: Float,
        hasObfuText: Boolean,
        val comp: Component,
    ) : StylizedTextHud.LineData(
        width,
        visualWidth,
        ascent,
        descent,
        hasObfuText,
    )
}