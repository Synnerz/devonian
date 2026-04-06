package com.github.synnerz.devonian.hud.texthud

import com.github.synnerz.devonian.api.bufimgrenderer.BufferedImageFactory
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.utils.math.MathUtils
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.awt.Font
import java.awt.Graphics2D
import java.awt.GraphicsEnvironment
import java.awt.RenderingHints
import kotlin.math.ceil

class BImgTextHudRenderer(name: String) : IStylizedTextHudRenderer(name), FontListener {
    private var disposed = false

    private var fontMain: Font? = null
    private var fontMono: Font? = null
    private var fontBack: Font? = null

    private val renderer = TextRenderer(name)

    init {
        registerFontListener(this)
    }

    override fun onFontChange(f: Font) {
        if (disposed) return
        parent.markFont()
    }

    override fun onFontUpdate() {
        fontMain = fontMainBase.deriveFont(Font.PLAIN, parent.fontSize)
        fontMono = Font(Font.MONOSPACED, Font.PLAIN, (parent.fontSize + 0.5f).toInt())
        fontBack = Font(Font.SANS_SERIF, Font.PLAIN, (parent.fontSize + 0.5f).toInt())
    }

    private var cachedGraphics: Graphics2D? = null

    override fun onUpdateLine(
        str: String,
        params: StylizedTextHud.TextRenderParams
    ): StylizedTextHud.LineData {
        if (cachedGraphics == null) {
            cachedGraphics = BufferedImageFactory.BLANK_IMAGE.createGraphics()
            cachedGraphics!!.font = fontMain
            cachedGraphics!!.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)
            cachedGraphics!!.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF)
        }

        return StringParser.processString(
            str,
            cachedGraphics!!,
            fontMain!!,
            fontMono!!,
            fontBack!!,
            params.fontSize,
        )
    }

    override fun updateCleanup() {
        cachedGraphics?.dispose()
        cachedGraphics = null

        if (parent.hasObfuText) parent.markText()
    }

    private var maxW = 0
    private var maxH = 0
    private var age = 0
    override fun onUpdateImage() {
        val params = parent.lastRenderParams
        val actualW = ceil(parent.lineWidth).toInt()
        val actualH = ceil(params.fontSize * (parent.lines.size + 1) + params.shadow.getSizeIncrease(params.fontSize)).toInt()
        val scaleW = MathUtils.ceilPow2(actualW, 1)
        val scaleH = MathUtils.ceilPow2(actualH, 2)
        if (age > 0 && scaleW <= maxW && scaleH <= maxH) {
            if (scaleW < maxW || scaleH < maxH) age--
            else age = 10
        } else {
            maxW = scaleW
            maxH = scaleH
            age = 10
        }
        renderer.update(
            maxW, maxH,
            TextRenderer.RenderParams(
                params,
                parent.lines.map { it.data as StringParser.LayoutLineData },
                parent.lineWidth,
                parent.fontAscent,
                parent.fontDescent,
            )
        )
    }

    override fun renderText(ctx: GuiGraphicsExtractor) {
        val pos = parent.getBounds()
        renderer.draw(ctx, pos.x.toFloat(), pos.y.toFloat(), parent.renderScale.toFloat())
    }

    override fun dispose() {
        if (disposed) return
        disposed = true
        renderer.dispose()
    }

    fun invalidate() {
        renderer.invalidate()
    }

    companion object {
        private val customFonts = linkedMapOf(
            "Mojangles" to "/assets/devonian/Mojangles.ttf",
            "CherryBombOne" to "/assets/devonian/CherryBombOne-Regular.ttf",
        ).entries.associateTo(linkedMapOf()) { (name, path) ->
            name to Font.createFont(
                Font.TRUETYPE_FONT, this::class.java.getResourceAsStream(path)!!
            )!!
        }
        val Fonts = linkedMapOf<String, Font>()
        private val Instances = mutableListOf<FontListener>()

        fun registerFontListener(listener: FontListener) {
            Instances.add(listener)
        }

        var fontMainBase: Font = customFonts.firstEntry().value
            private set
        var fontMainName = customFonts.firstEntry().key!!
            private set

        fun setActiveFont(fontName: String) {
            if (fontName == fontMainName) return
            val font = Fonts[fontName] ?: return
            fontMainName = fontName
            fontMainBase = font
            Instances.forEach { it.onFontChange(font) }
        }

        init {
            customFonts.forEach { (name, font) ->
                Fonts[name] = font
            }

            for (f in GraphicsEnvironment.getLocalGraphicsEnvironment().allFonts) {
                Fonts.putIfAbsent(f.family.replace(" ", ""), f)
            }

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