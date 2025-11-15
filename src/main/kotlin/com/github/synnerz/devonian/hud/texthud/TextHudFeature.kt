package com.github.synnerz.devonian.hud.texthud

import com.github.synnerz.devonian.api.bufimgrenderer.BufferedImageFactoryImpl
import com.github.synnerz.devonian.hud.HudFeature
import com.github.synnerz.devonian.utils.BoundingBox
import com.github.synnerz.devonian.utils.JsonUtils
import com.github.synnerz.devonian.utils.Render2D
import com.github.synnerz.devonian.utils.math.MathUtils
import net.minecraft.client.gui.DrawContext
import org.lwjgl.glfw.GLFW
import java.awt.*
import java.util.*
import kotlin.math.ceil
import kotlin.math.max

abstract class TextHudFeature(
    configName: String, area: String? = null, subarea: String? = null,
    hudConfigName: String = configName[0].uppercase() + configName.substring(1)
) : HudFeature(configName, area, subarea, hudConfigName) {
    init {
        Instances.add(this)
    }

    abstract fun getEditText(): List<String>

    private var textLines = mutableListOf<Line>()
    private var editLines = mutableListOf<Line>()

    private var imageDirty = false
    private var fontsDirty = true

    private fun markImage() {
        imageDirty = true
    }

    private fun markText(lines: List<Line>) {
        lines.forEach { it.dirty = true }
    }

    private fun markFont() {
        fontsDirty = true
    }

    private var anchor = Anchor.NW
    private var align = Align.Left
    private var shadow = true
        set(value) {
            if (field != value) {
                markText(textLines)
                markText(editLines)
            }
            field = value
        }
    private var backdrop = Backdrop.None

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

    private var lineWidth = 0f
    private var lineVisualWidth = 0f
    private var hasObfuText = false
    private var drawnLines = 0
    private var wasLastDrawSample = false
    private var renderScale = 1f

    fun getWidth() = lineWidth.toDouble() * renderScale

    fun getLineHeight() = MC_FONT_SIZE * scale.toDouble()

    fun getHeight() = drawnLines * getLineHeight()

    override fun getBounds(): BoundingBox =
        BoundingBox(
            if (align == Align.CenterIgnoreAnchor) x - getWidth() * 0.5
            else when (anchor) {
                Anchor.NW, Anchor.SW -> x.toDouble()
                Anchor.NE, Anchor.SE -> x - getWidth()
            },
            when (anchor) {
                Anchor.NW, Anchor.NE -> y.toDouble()
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
        TextRenderer.TextRenderParams(align, shadow, backdrop, fontSize, fontMainBase, emptyList(), 0f)
    private val renderer = TextRenderer(configName)

    private fun updateWithLines(lines: List<Line>) {
        val window = minecraft.window
        val renderSize = MC_FONT_SIZE * window.scaleFactor.toFloat() * scale
        if (renderSize != fontSize) markFont()
        if (fontsDirty) {
            fontsDirty = false
            markText(lines)
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
                    g!!.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                }

                it.data = StringParser.processString(it.str, shadow, g!!, fontMain!!, fontMono!!, fontBack!!, fontSize)
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

    private fun drawWithLines(ctx: DrawContext, lines: List<Line>) {
        drawnLines = lines.size
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

    override fun drawImpl(ctx: DrawContext) {
        if (wasLastDrawSample) markImage()
        updateWithLines(textLines)
        drawWithLines(ctx, textLines)
        wasLastDrawSample = false
    }

    override fun sampleDraw(ctx: DrawContext, mx: Int, my: Int, selected: Boolean) {
        if (!wasLastDrawSample) markImage()
        setLinesTo(getEditText(), editLines)
        updateWithLines(editLines)
        drawWithLines(ctx, editLines)
        wasLastDrawSample = true

        super.sampleDraw(ctx, mx, my, selected)

        Render2D.drawCircle(ctx, (x + 0.5).toInt(), (y + 0.5).toInt(), 3, Color.RED)
    }

    private fun clearLinesOf(lines: MutableList<Line>) {
        lines.clear()
        imageDirty = false
        hasObfuText = false
        lineWidth = 0f
        lineVisualWidth = 0f
    }

    fun clearLines() = apply {
        clearLinesOf(textLines)
    }

    fun addLine(s: String) = apply {
        markImage()
        textLines.add(Line(s))
    }

    fun addLines(s: List<String>) = apply {
        markImage()
        s.forEach { textLines.add(Line(it)) }
    }

    fun setLineTo(s: String, lines: MutableList<Line>) {
        if (lines.size == 1 && lines[0].str == s) return
        clearLinesOf(lines)
        markImage()
        lines.add(Line(s))
    }

    fun setLine(s: String) = apply {
        setLineTo(s, textLines)
    }

    private fun setLinesTo(s: List<String>, lines: MutableList<Line>) {
        when (s.size) {
            0 -> clearLinesOf(lines)
            1 -> setLineTo(s[0], lines)
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

    fun setLines(s: List<String>) = apply { setLinesTo(s, textLines) }

    fun removeLine(i: Int) = apply {
        if (i !in textLines.indices) return@apply
        markImage()
        textLines.removeAt(i)
    }

    override fun onKeyPress(keyCode: Int) {
        super.onKeyPress(keyCode)

        when (keyCode) {
            GLFW.GLFW_KEY_1 -> anchor = anchor.cycle()
            GLFW.GLFW_KEY_2 -> align = align.cycle()
            GLFW.GLFW_KEY_3 -> shadow = !shadow
            GLFW.GLFW_KEY_4 -> backdrop = backdrop.cycle()
        }
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
        } catch (e: Exception) {
            GraphicsEnvironment.getLocalGraphicsEnvironment().allFonts[0]
        }
        val Fonts = mutableMapOf<String, Font>()
        private val Instances = mutableListOf<TextHudFeature>()

        private var fontMainBase: Font = MOJANGLES
        fun setActiveFont(f: Font) {
            if (fontMainBase == f) return
            fontMainBase = f
            Instances.forEach { it.markFont() }
        }

        init {
            for (f in GraphicsEnvironment.getLocalGraphicsEnvironment().allFonts) {
                Fonts[f.family.replace(" ", "")] = f
            }
            Fonts["Mojangles"] = MOJANGLES

            JsonUtils.afterLoad {
                val fontName = JsonUtils.get<String?>("textHudFont")
                if (fontName != null) Fonts[fontName]?.let { setActiveFont(it) }
            }
        }
    }
}