package com.github.synnerz.devonian.hud.texthud

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.features.HudManagerRenderer
import com.github.synnerz.devonian.utils.BoundingBox
import net.minecraft.client.gui.GuiGraphicsExtractor
import kotlin.math.max

open class StylizedTextHud(
    val name: String,
    protected val data: DataProvider = StaticProvider(
        0.0, 0.0, 1f,
        Anchor.NW,
        Align.Left,
        Shadow.None,
        Backdrop.None
    ),
    renderer: IStylizedTextHudRenderer = getRenderer(name),
    overrideRenderer: Boolean = true,
) : ITextHud, DataProvider by data {
    init {
        if (overrideRenderer) instances.add(this)
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

    private var hasText = false
    private var imageDirty = false
    private var fontsDirty = true

    fun markImage() {
        imageDirty = true
    }

    open fun markText() {
        lines.forEach { it.dirty = true }
    }

    fun markFont() {
        fontsDirty = true
    }

    protected var fontSize = BASE_FONT_SIZE
        private set
    protected var lineSpacingInternal = 1f
    protected var lineWidth = 0f
    protected var fontAscent = 0f
    protected var fontDescent = 0f
    var renderScale = 1.0
        private set
    var hasObfuText = false
        private set

    protected var lastRenderParams = TextRenderParams(align, shadow, backdrop, fontSize, fontSize * lineSpacingInternal)
        private set

    override fun getWidth(): Double = renderer.getRenderMetrics().width * renderScale

    override fun getLineHeight(): Double = renderer.getRenderMetrics().fontSize * renderScale

    override fun getHeight(): Double =
        if (lines.isEmpty()) 0.0
        else lines.size * getLineHeight() + renderer.getRenderMetrics().descent * renderScale

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
            renderer.onFontUpdate(fontSize)
        }

        renderScale = 1.0 / window.guiScale

        val currentParams = TextRenderParams(align, shadow, backdrop, fontSize, fontSize * lineSpacingInternal)
        if (currentParams != lastRenderParams) markImage()
        if (currentParams.shadow != lastRenderParams.shadow) markText()
        lastRenderParams = currentParams

        lineWidth = 0f
        fontAscent = 0f
        fontDescent = 0f
        hasObfuText = false
        renderer.onUpdate()

        updateLines()

        renderer.updateCleanup()
    }

    protected open fun updateLines() {
        lines.forEach {
            if (it.dirty) {
                it.data = renderer.onUpdateLine(it.str, lastRenderParams)
                if (it.data!!.width != 0f) it.data!!.width += shadow.getSizeIncrease(fontSize)
                it.dirty = false
                markImage()
            }

            lineWidth = max(lineWidth, it.data!!.width)
            fontAscent = max(fontAscent, it.data!!.ascent)
            fontDescent = max(fontDescent, it.data!!.descent)
            if (it.data!!.hasObfuText) hasObfuText = true
        }
    }

    override fun draw(ctx: GuiGraphicsExtractor) {
        if (!hasText) return
        update()
        if (lines.isNotEmpty()) {
            if (imageDirty) {
                imageDirty = false
                val metrics = TextRenderMetrics(
                    lineWidth,
                    fontAscent, fontDescent,
                    fontSize,
                )
                renderer.onUpdateImage(lastRenderParams, metrics)
            }
            renderText(ctx)
        }
    }

    protected open fun renderText(ctx: GuiGraphicsExtractor) {
        renderer.renderText(ctx)
    }

    override fun clearLines(): ITextHud = apply {
        lines.clear()
        hasText = false
    }

    override fun addLine(s: String): ITextHud = apply {
        lines.add(Line(s))
        markImage()
        hasText = true
    }

    override fun addLines(s: List<String>): ITextHud = apply {
        if (s.isEmpty()) return@apply
        s.forEach { lines.add(Line(it)) }
        markImage()
        hasText = true
    }

    override fun setLine(s: String): ITextHud = apply {
        if (lines.size == 1 && lines[0].str == s) return@apply
        lines.clear()
        lines.add(Line(s))
        markImage()
        hasText = true
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
                hasText = true
            }
        }
    }

    private var storedLines = emptyList<String>()
    override fun storeLines(): ITextHud = apply {
        storedLines = lines.map { it.str }
    }

    override fun resetLines(): ITextHud = apply {
        setLines(storedLines)
        storedLines = emptyList()
    }

    override fun invalidateLines(): ITextHud = apply {
        hasText = false
        renderer.invalidate()
    }

    open class LineData(
        width: Float,
        ascent: Float,
        descent: Float,
        val hasObfuText: Boolean,
    ) : FontMetrics(
        width,
        ascent,
        descent,
    )

    open class FontMetrics(
        var width: Float,
        val ascent: Float,
        val descent: Float,
    )

    class Line(val str: String, var data: LineData? = null, var dirty: Boolean = true)

    data class TextRenderParams(
        val align: Align,
        val shadow: Shadow,
        val backdrop: Backdrop,
        val fontSize: Float,
        val lineSpacing: Float,
    )

    open class TextRenderMetrics(
        width: Float,
        ascent: Float,
        descent: Float,
        val fontSize: Float,
    ) : FontMetrics(width, ascent, descent) {
        companion object {
            val EMPTY = TextRenderMetrics(0f, 0f, 0f, 0f)
        }
    }

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

    enum class Shadow(private val mul: Float) {
        None(0f), Drop(0.1f), Outline(0.2f);

        fun cycle() = when (this) {
            None -> Drop
            Drop -> Outline
            Outline -> None
        }

        fun getSizeIncrease(fontSize: Float) = fontSize * mul

        companion object {
            fun from(ordinal: Int) = entries.getOrElse(ordinal) { None }
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
                if (it.renderer is BImgTextHudRenderer == bimg) return@forEach
                it.renderer = getRenderer(it.name, bimg)
            }
        }
    }
}