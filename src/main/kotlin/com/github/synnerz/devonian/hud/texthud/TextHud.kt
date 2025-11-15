package com.github.synnerz.devonian.hud.texthud

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.bufimgrenderer.BufferedImageFactoryImpl
import com.github.synnerz.devonian.utils.BoundingBox
import com.github.synnerz.devonian.utils.JsonUtils
import com.github.synnerz.devonian.utils.math.MathUtils
import net.minecraft.client.gui.DrawContext
import java.awt.Font
import java.awt.Graphics2D
import java.awt.GraphicsEnvironment
import java.awt.RenderingHints
import java.util.*
import kotlin.math.ceil
import kotlin.math.max

class TextHud(val name: String, private val data: DataProvider) : ITextHud, DataProvider by data, FontListener {
    init {
        registerFontListener(this)
    }

    private var lines = mutableListOf<Line>()

    private var imageDirty = false
    private var fontsDirty = true

    private fun markImage() {
        imageDirty = true
    }

    private fun markText() {
        lines.forEach { it.dirty = true }
    }

    private fun markFont() {
        fontsDirty = true
    }

    override fun onFontChange(f: Font) {
        markFont()
    }

    override var shadow: Boolean = data.shadow
        set(value) {
            if (field != value) markText()
            field = value
        }

    private var lineWidth = 0f
    private var lineVisualWidth = 0f
    private var hasObfuText = false
    private var renderScale = 1f

    override fun getWidth() = lineWidth.toDouble() * renderScale

    override fun getLineHeight() = MC_FONT_SIZE * scale.toDouble()

    override fun getHeight() = lines.size * getLineHeight()

    override fun getBounds(): BoundingBox =
        BoundingBox(
            if (align == Align.CenterIgnoreAnchor) x - getWidth() * 0.5
            else when (anchor) {
                Anchor.NW, Anchor.SW -> x
                Anchor.NE, Anchor.SE -> x - getWidth()
            },
            when (anchor) {
                Anchor.NW, Anchor.NE -> y
                Anchor.SW, Anchor.SE -> y - getHeight()
            },
            getWidth(),
            getHeight()
        )

    private var fontSize: Float = MC_FONT_SIZE
    private var fontMain: Font? = null
    private var fontMono: Font? = null
    private var fontBack: Font? = null
    private var lastImageParams =
        TextRenderer.TextRenderParams(
            align, shadow, backdrop, fontSize,
            fontMainBase, emptyList(), 0f
        )
    private val renderer = TextRenderer(name)

    private fun update() {
        val window = Devonian.minecraft.window
        val renderSize = MC_FONT_SIZE * window.scaleFactor.toFloat() * scale
        if (renderSize != fontSize) markFont()
        if (fontsDirty) {
            fontsDirty = false
            markText()
            fontSize = renderSize
            fontMain = fontMainBase.deriveFont(Font.PLAIN, fontSize)
            fontMono = Font(Font.MONOSPACED, Font.PLAIN, (fontSize + 0.5f).toInt())
            fontBack = Font(Font.SANS_SERIF, Font.PLAIN, (fontSize + 0.5f).toInt())
        }

        renderScale = 1f / window.scaleFactor.toFloat()

        var g: Graphics2D? = null
        lineWidth = 0f
        lineVisualWidth = 0f
        hasObfuText = false

        lines.forEach {
            if (it.dirty) {
                if (g == null) {
                    g = BufferedImageFactoryImpl.BLANK_IMAGE.createGraphics()
                    g!!.font = fontMain
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                }

                it.data = StringParser.processString(it.str, shadow, g, fontMain!!, fontMono!!, fontBack!!, fontSize)
                it.dirty = false
                markImage()
            }

            lineWidth = max(lineWidth, it.data!!.width)
            lineVisualWidth = max(lineVisualWidth, it.data!!.width)
            if (it.data!!.obfData.isNotEmpty()) {
                hasObfuText = true
                it.dirty = true
            }
        }

        g?.dispose()

        val currentParams = TextRenderer.TextRenderParams(
            align,
            shadow,
            backdrop,
            fontSize,
            fontMain!!,
            lines.map { it.data!! },
            lineVisualWidth
        )
        if (!imageDirty && currentParams != lastImageParams) markImage()
        lastImageParams = currentParams
    }

    private fun drawImage(ctx: DrawContext) {
        if (lines.isEmpty()) return

        if (imageDirty) {
            imageDirty = false
            val actualW = ceil(lineWidth + (if (shadow) fontSize * 0.1 else 0.0)).toInt()
            val actualH = ceil(fontSize * (lines.size + 1) + (if (shadow) fontSize * 0.1 else 0.0)).toInt()
            renderer.update(MathUtils.ceilPow2(actualW, 1), MathUtils.ceilPow2(actualH, 2), lastImageParams)
        }

        val pos = getBounds()
        renderer.draw(ctx, pos.x.toFloat(), pos.y.toFloat(), renderScale)
    }

    override fun draw(ctx: DrawContext) {
        update()
        drawImage(ctx)
    }

    override fun clearLines() = apply {
        lines.clear()
        imageDirty = false
        hasObfuText = false
        lineWidth = 0f
        lineVisualWidth = 0f
    }

    override fun addLine(s: String) = apply {
        markImage()
        lines.add(Line(s))
    }

    override fun addLines(s: List<String>) = apply {
        markImage()
        s.forEach { lines.add(Line(it)) }
    }

    override fun setLine(s: String) = apply {
        if (lines.size == 1 && lines[0].str == s) return@apply
        clearLines()
        markImage()
        lines.add(Line(s))
    }

    override fun setLines(s: List<String>) = apply {
        when (s.size) {
            0 -> clearLines()
            1 -> setLine(s[0])
            else -> {
                if (s.size < lines.size) {
                    markImage()
                    lines.dropLast(lines.size - s.size)
                }
                s.forEachIndexed { i, v ->
                    if (i < lines.size && lines[i].str == v) return@forEachIndexed
                    markImage()
                    val l = Line(v)
                    if (i < lines.size) lines[i] = l
                    else lines.add(l)
                }
            }
        }
    }

    override fun removeLine(i: Int) = apply {
        if (i !in lines.indices) return@apply
        markImage()
        lines.removeAt(i)
    }

    data class Line(val str: String, var data: StringParser.LineData? = null, var dirty: Boolean = true)

    enum class Anchor {
        NW, NE,
        SW, SE;

        fun cycle() = when (this) {
            NW -> NE
            NE -> SW
            SW -> SE
            SE -> NW
        }

        companion object {
            fun from(ordinal: Int) = entries.getOrElse(ordinal) { NW }
        }
    }

    enum class Align {
        Left, Right, Center, CenterIgnoreAnchor;

        fun cycle() = when (this) {
            Left -> Right
            Right -> Center
            Center -> CenterIgnoreAnchor
            CenterIgnoreAnchor -> Left
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
        const val MC_FONT_SIZE = 10f
        val MOJANGLES: Font = try {
            Font.createFont(
                Font.TRUETYPE_FONT, Objects.requireNonNull(
                    this::class.java.getResourceAsStream("/assets/devonian/Mojangles.ttf")
                )
            )
        } catch (_: Exception) {
            GraphicsEnvironment.getLocalGraphicsEnvironment().allFonts[0]
        }
        val Fonts = mutableMapOf<String, Font>()
        private val Instances = mutableListOf<FontListener>()

        fun registerFontListener(listener: FontListener) {
            Instances.add(listener)
        }

        private var fontMainBase: Font = MOJANGLES
        private var fontMainName = "Mojangles"
        fun setActiveFont(fontName: String) {
            if (fontName == fontMainName) return
            val font = Fonts[fontName] ?: return
            fontMainName = fontName
            fontMainBase = font
            Instances.forEach { it.onFontChange(font) }
        }

        init {
            for (f in GraphicsEnvironment.getLocalGraphicsEnvironment().allFonts) {
                Fonts[f.family.replace(" ", "")] = f
            }
            Fonts["Mojangles"] = MOJANGLES

            JsonUtils.afterLoad {
                val fontName = JsonUtils.get<String?>("textHudFont")
                if (fontName != null) setActiveFont(fontName)
            }

            JsonUtils.preSave {
                JsonUtils.set("textHudFont", fontMainName)
            }
        }
    }
}