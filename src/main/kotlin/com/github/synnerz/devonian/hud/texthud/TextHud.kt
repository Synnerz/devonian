package com.github.synnerz.devonian.hud.texthud

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.bufimgrenderer.BufferedImageFactoryImpl
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.utils.BoundingBox
import com.github.synnerz.devonian.utils.TextRendererImpl.TextRenderParams
import com.github.synnerz.devonian.utils.math.MathUtils
import net.minecraft.client.gui.GuiGraphics
import java.awt.Font
import java.awt.Graphics2D
import java.awt.GraphicsEnvironment
import java.awt.RenderingHints
import java.util.*
import kotlin.math.ceil
import kotlin.math.max

open class TextHud(val name: String, private val data: DataProvider) : ITextHud, DataProvider by data, FontListener {
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

    private var lineWidth = 0f
    private var lineVisualWidth = 0f
    private var fontAscent = 0f
    private var fontDescent = 0f
    private var hasObfuText = false
    private var renderScale = 1f

    override fun getWidth() = lineWidth.toDouble() * renderScale

    override fun getLineHeight() = fontSize.toDouble() * renderScale

    override fun getHeight() =
        if (lines.isEmpty()) 0.0 else (lines.size - 1) * getLineHeight() + (fontAscent + fontDescent) * renderScale

    override fun getBounds(): BoundingBox = anchor.transform(
        BoundingBox(
            x, y,
            getWidth(), getHeight()
        )
    )

    private var fontSize: Float = MC_FONT_SIZE
    private var fontMain: Font? = null
    private var fontMono: Font? = null
    private var fontBack: Font? = null
    private var lastImageParams =
        TextRenderParams(
            align, shadow, backdrop, fontSize,
            fontMainBase, emptyList(), 0f
        )
    protected var rendererInit = false
    protected val renderer by lazy {
        rendererInit = true
        TextRenderer(name)
    }

    private fun update() {
        val window = Devonian.minecraft.window
        val renderSize = MC_FONT_SIZE * window.guiScale.toFloat() * scale
        if (renderSize != fontSize) markFont()
        if (fontsDirty) {
            fontsDirty = false
            markText()
            fontSize = renderSize
            fontMain = fontMainBase.deriveFont(Font.PLAIN, fontSize)
            fontMono = Font(Font.MONOSPACED, Font.PLAIN, (fontSize + 0.5f).toInt())
            fontBack = Font(Font.SANS_SERIF, Font.PLAIN, (fontSize + 0.5f).toInt())
        }

        renderScale = 1f / window.guiScale.toFloat()

        if (lastImageParams.shadow != shadow) markText()

        var g: Graphics2D? = null
        lineWidth = 0f
        lineVisualWidth = 0f
        fontAscent = 0f
        fontDescent = 0f
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
            fontAscent = max(fontAscent, it.data!!.ascent)
            fontDescent = max(fontDescent, it.data!!.descent)
            if (it.data!!.obfData.isNotEmpty()) {
                hasObfuText = true
                it.dirty = true
            }
        }

        g?.dispose()

        val currentParams = TextRenderParams(
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

    private fun drawImage(ctx: GuiGraphics) {
        if (lines.isEmpty()) return

        if (imageDirty) {
            imageDirty = false
            val actualW = ceil(lineWidth + (if (shadow) fontSize * 0.1 else 0.0)).toInt()
            val actualH = ceil(fontSize * (lines.size + 1) + (if (shadow) fontSize * 0.1 else 0.0)).toInt()
            renderer.update(MathUtils.ceilPow2(actualW, 1), MathUtils.ceilPow2(actualH, 2), lastImageParams)
        }

        drawCachedImage(ctx)
    }

    protected open fun drawCachedImage(ctx: GuiGraphics) {
        val pos = getBounds()
        renderer.draw(ctx, pos.x.toFloat(), pos.y.toFloat(), renderScale)
    }

    override fun draw(ctx: GuiGraphics) {
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
                    lines.subList(s.size, lines.size).clear()
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

    fun dispose() {
        if (rendererInit) renderer.dispose()
    }

    data class Line(val str: String, var data: StringParser.LineData? = null, var dirty: Boolean = true)

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
        const val MC_FONT_SIZE = 10f
        val MAIN_FONT: Font = try {
            Font.createFont(
                Font.TRUETYPE_FONT, Objects.requireNonNull(
                    this::class.java.getResourceAsStream("/assets/devonian/CherryBombOne-Regular.ttf")
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

        var fontMainBase: Font = MAIN_FONT
            private set
        var fontMainName = "CherryBombOne"
            private set

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
            Fonts["CherryBombOne"] = MAIN_FONT

            // TODO: setting
            Config.onAfterLoad {
                val fontName = Config.get<String?>("textHudFont")
                if (fontName != null) setActiveFont(fontName)
            }

            Config.onPreSave {
                Config.set("textHudFont", fontMainName)
            }
        }
    }
}