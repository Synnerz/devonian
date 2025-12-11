package com.github.synnerz.devonian.hud.texthud

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.utils.BoundingBox
import net.minecraft.sounds.SoundEvents

object Alert : StylizedTextHud("internal_devonian_alert") {
    private val soundEvent = SoundEvents.ANVIL_PLACE
    private var clearTime = 0L
    private var needRescale = false

    @JvmOverloads
    fun show(text: String, durationMs: Int, playSound: Boolean = true) {
        Scheduler.scheduleTask {
            val mc = Devonian.minecraft
            val window = mc.window

            val lines = text.split('\n').map { "&c$it" }

            x = window.guiScaledWidth * 0.5
            y = window.guiScaledHeight * 0.5
            clearLines()
            setLines(lines)
            needRescale = true

            clearTime = System.currentTimeMillis() + durationMs
            if (playSound) mc?.player?.playSound(soundEvent, 1f, 1f)

            (renderer as? BImgTextHudRenderer)?.invalidate()
        }
    }

    override fun update() {
        super.update()
        if (!needRescale) return

        val mc = Devonian.minecraft
        val window = mc.window
        val bb = BoundingBox(
            window.guiScaledWidth * 0.3,
            window.guiScaledHeight * 0.3,
            window.guiScaledWidth * 0.4,
            window.guiScaledHeight * 0.4
        )

        scale = getBounds().fitInside(bb).first.toFloat()

        super.update()
        needRescale = false
    }

    fun initialize() {
        EventBus.on<RenderOverlayEvent> { event ->
            if (clearTime == 0L) return@on
            val t = System.currentTimeMillis()
            if (t > clearTime) clearTime = 0L
            else draw(event.ctx)
        }

        anchor = Anchor.Center
        align = Align.Center
        shadow = true
        backdrop = Backdrop.None
    }
}