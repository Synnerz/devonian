package com.github.synnerz.devonian.hud.texthud

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.bufimgrenderer.BufferedImageFactoryImpl
import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.utils.BoundingBox
import net.minecraft.sounds.SoundEvents
import java.awt.Font

object Alert : SimpleTextHud("internal_devonian_alert") {
    private val soundEvent = SoundEvents.ANVIL_PLACE
    private var clearTime = 0L
    private val bimg = BufferedImageFactoryImpl().create(1, 1)

    @JvmOverloads
    fun show(text: String, durationMs: Int, playSound: Boolean = true) {
        Scheduler.scheduleTask {
            val mc = Devonian.minecraft

            val window = mc.window
            val bb = BoundingBox(
                window.guiScaledWidth * 0.125,
                window.guiScaledHeight * 0.125,
                window.guiScaledWidth * 0.75,
                window.guiScaledHeight * 0.75
            )

            val str = "&c$text"

            val font = fontMainBase.deriveFont(Font.PLAIN, MC_FONT_SIZE)
            val g = bimg.createGraphics()
            g.font = font
            val line = StringParser.processString(str, shadow, g, font, font, font, MC_FONT_SIZE)
            val (f, pos) = BoundingBox(
                0.0, 0.0,
                line.visualWidth.toDouble(), (line.ascent + line.descent).toDouble()
            ).fitInside(bb)

            x = window.guiScaledWidth * 0.5
            y = window.guiScaledHeight * 0.5 + line.ascent * f
            scale = f.toFloat()
            setLine(str)

            clearTime = System.currentTimeMillis() + durationMs
            if (playSound) mc?.player?.playSound(soundEvent, 1f, 1f)
            renderer.invalidate()
        }
    }

    fun initialize() {
        EventBus.on<RenderOverlayEvent> { event ->
            if (clearTime == 0L) return@on
            val t = System.currentTimeMillis()
            if (t > clearTime) clearTime = 0L
            else draw(event.ctx)
        }

        anchor = Anchor.SW
        align = Align.CenterIgnoreAnchor
        shadow = true
        backdrop = Backdrop.None
    }
}