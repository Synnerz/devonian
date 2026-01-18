package com.github.synnerz.devonian.hud.texthud

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.utils.BoundingBox
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.sounds.SoundEvents
import kotlin.math.min

object Alert : StylizedTextHud("internal_devonian_alert") {
    private val soundEvent = SoundEvents.ANVIL_PLACE
    private var clearTime = 0L
    private var needRescale = false

    @JvmOverloads
    fun show(text: String, durationMs: Int, playSound: Boolean = true) {
        Scheduler.scheduleTask {
            val mc = Devonian.minecraft

            if (durationMs > 0) {
                val window = mc.window

                val lines = text.split('\n').map { "&c$it" }

                x = window.guiScaledWidth * 0.5
                y = window.guiScaledHeight * 0.5
                clearLines()
                setLines(lines)
                needRescale = true

                clearTime = System.currentTimeMillis() + durationMs
            }

            if (playSound) mc?.player?.playSound(soundEvent, 1f, 1f)
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

        val f = getBounds().fitInside(bb).first.toFloat()
        if (f != 1f) {
            scale = min(5f, scale * f)
            (renderer as? BImgTextHudRenderer)?.invalidate()
            super.update()
        }

        needRescale = false
    }

    override fun renderText(ctx: GuiGraphics) {
        if (needRescale) return
        super.renderText(ctx)
    }

    fun initialize() {
        EventBus.on<RenderOverlayEvent> { event ->
            if (clearTime == 0L) return@on
            val t = System.currentTimeMillis()
            if (t > clearTime) clearTime = 0L
            else draw(event.ctx)
        }

        DevonianCommand.command.subcommand("alert") { _, args ->
            show(
                args[0] as String,
                args.getOrNull(1) as Int? ?: 1000,
                args.getOrNull(2) as Boolean? ?: true,
            )
            return@subcommand 1
        }
            .string("msg")
            .integer("time", 0)
            .bool("sound")

        anchor = Anchor.Center
        align = Align.Center
        shadow = Shadow.Drop
        backdrop = Backdrop.None
    }
}