package com.github.synnerz.devonian.hud.texthud

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.features.HudManagerRenderer
import com.github.synnerz.devonian.utils.BoundingBox
import net.minecraft.client.gui.GuiGraphics
import kotlin.math.max

open class StylizedTextHud(
    val name: String,
    protected val data: DataProvider = StaticProvider(
        0.0, 0.0, 1f,
        Anchor.NW,
        Align.Left,
        false,
        Backdrop.None
    ),
    renderer: IStylizedTextHudRenderer = getRenderer(name),
) : ITextHud, DataProvider by data {
    init {
        instances.add(this)
        renderer.parent = this
    }

    protected var renderer: IStylizedTextHudRenderer = renderer
        set(value) {
            field.dispose()
            markImage()
            markText()
            markFont()
            value.parent = this
            field = value
        }

    val lines = mutableListOf<Line>()

    private var imageDirty = false
    private var fontsDirty = true

    fun markImage() {
        imageDirty = true
    }

    fun markText() {
        lines.forEach { it.dirty = true }
    }

    fun markFont() {
        fontsDirty = true
    }

    var fontSize = BASE_FONT_SIZE
        private set
    var lineWidth = 0f
        private set
    var lineVisualWidth = 0f
        private set
    var fontAscent = 0f
        private set
    var fontDescent = 0f
        private set
    var renderScale = 1.0
        private set
    var hasObfuText = false
        private set

    var lastRenderParams = TextRenderParams(align, shadow, backdrop, fontSize)
        private set

    override fun getWidth(): Double = lineWidth * renderScale

    override fun getLineHeight(): Double = fontSize * renderScale

    override fun getHeight(): Double =
        if (lines.isEmpty()) 0.0 else (lines.size - 1) * getLineHeight() + (fontAscent + fontDescent) * renderScale

    override fun getBounds(): BoundingBox = anchor.transform(
        BoundingBox(
            x, y,
            getWidth(), getHeight()
        )
    )

    protected open fun update() {
        val window = Devonian.minecraft.window
        val renderSize = BASE_FONT_SIZE * window.guiScale.toFloat() * scale
        if (renderSize != fontSize) markFont()
        if (fontsDirty) {
            fontsDirty = false
            markText()
            fontSize = renderSize
            renderer.onFontUpdate()
        }

        renderScale = 1.0 / window.guiScale

        if (lastRenderParams.shadow != shadow) markText()

        val currentParams = TextRenderParams(align, shadow, backdrop, fontSize)
        if (currentParams != lastRenderParams) markImage()
        lastRenderParams = currentParams

        lineWidth = 0f
        lineVisualWidth = 0f
        fontAscent = 0f
        fontDescent = 0f
        hasObfuText = false
        renderer.onUpdate()

        lines.forEach {
            if (it.dirty) {
                it.data = renderer.onUpdateLine(it.str, currentParams)
                it.dirty = false
                markImage()
            }

            lineWidth = max(lineWidth, it.data!!.width)
            lineVisualWidth = max(lineVisualWidth, it.data!!.visualWidth)
            fontAscent = max(fontAscent, it.data!!.ascent)
            fontDescent = max(fontDescent, it.data!!.descent)
            if (it.data!!.hasObfuText) hasObfuText = true
        }

        if (lineVisualWidth != 0f) lineVisualWidth += (if (shadow) fontSize * 0.1f else 0.0f)

        renderer.updateCleanup()
    }

    override fun draw(ctx: GuiGraphics) {
        update()
        if (lines.isNotEmpty()) {
            if (imageDirty) {
                imageDirty = false
                renderer.onUpdateImage()
            }
            renderText(ctx)
        }
    }

    protected open fun renderText(ctx: GuiGraphics) {
        renderer.renderText(ctx)
    }

    override fun clearLines(): ITextHud = apply {
        lines.clear()
    }

    override fun addLine(s: String): ITextHud = apply {
        lines.add(Line(s))
        markImage()
    }

    override fun addLines(s: List<String>): ITextHud = apply {
        s.forEach { lines.add(Line(it)) }
        markImage()
    }

    override fun setLine(s: String): ITextHud = apply {
        if (lines.size == 1 && lines[0].str == s) return@apply
        lines.clear()
        lines.add(Line(s))
        markImage()
    }

    override fun setLines(s: List<String>): ITextHud = apply {
        when (s.size) {
            0 -> clearLines()
            1 -> setLine(s[0])
            else -> {
                if (s.size < lines.size) {
                    lines.subList(s.size, lines.size).clear()
                    markImage()
                }
                s.forEachIndexed { i, v ->
                    if (i < lines.size && lines[i].str == v) return@forEachIndexed
                    val l = Line(v)
                    if (i < lines.size) lines[i] = l
                    else lines.add(l)
                    markImage()
                }
            }
        }
    }

    override fun removeLine(i: Int): ITextHud = apply {
        if (i !in lines.indices) return@apply
        lines.removeAt(i)
        markImage()
    }

    open class LineData(
        width: Float,
        visualWidth: Float,
        ascent: Float,
        descent: Float,
        val hasObfuText: Boolean,
    ) : FontMetrics(
        width,
        visualWidth,
        ascent,
        descent,
    )

    open class FontMetrics(
        val width: Float,
        val visualWidth: Float,
        val ascent: Float,
        val descent: Float,
    )

    class Line(val str: String, var data: LineData? = null, var dirty: Boolean = true)

    data class TextRenderParams(
        val align: Align,
        val shadow: Boolean,
        val backdrop: Backdrop,
        val fontSize: Float,
    )

    enum class Anchor {
        NW, NE,
        SW, SE,
        Center;

        fun cycle() = when (this) {
            NW -> NE
            NE -> SW
            SW -> SE
            SE -> Center
            Center -> NW
        }

        fun transform(bb: BoundingBox) = BoundingBox(
            when (this) {
                NW, SW -> bb.x
                NE, SE -> bb.x - bb.w
                Center -> bb.x - bb.w * 0.5
            },
            when (this) {
                NW, NE -> bb.y
                SW, SE -> bb.y - bb.h
                Center -> bb.y - bb.h * 0.5
            },
            bb.w,
            bb.h
        )

        companion object {
            fun from(ordinal: Int) = entries.getOrElse(ordinal) { NW }
        }
    }

    enum class Align {
        Left, Right, Center;

        fun cycle() = when (this) {
            Left -> Right
            Right -> Center
            Center -> Left
        }

        companion object {
            fun from(ordinal: Int) = entries.getOrElse(ordinal) { Left }
        }
    }

    enum class Backdrop {
        None, Full, Line;

        fun cycle() = when (this) {
            None -> Full
            Full -> Line
            Line -> None
        }

        companion object {
            fun from(ordinal: Int) = entries.getOrElse(ordinal) { None }
        }
    }

    companion object {
        const val BASE_FONT_SIZE = 9f

        val instances = mutableListOf<StylizedTextHud>()

        @JvmStatic
        protected fun getRenderer(name: String, bimg: Boolean = HudManagerRenderer.isEnabled()): IStylizedTextHudRenderer {
            return if (bimg) BImgTextHudRenderer(name)
            else MCTextHudRenderer(name)
        }

        fun recreateRenderers(bimg: Boolean) {
            instances.forEach {
                it.renderer = getRenderer(it.name, bimg)
            }
        }
    }
}