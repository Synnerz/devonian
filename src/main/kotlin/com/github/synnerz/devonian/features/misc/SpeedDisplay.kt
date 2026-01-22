package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.TickEvent
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import net.minecraft.world.entity.ai.attributes.Attributes

object SpeedDisplay : TextHudFeature(
    "speedDisplay",
    "Shows current player speed.",
) {
    override fun getEditText(): List<String> = listOf("&f400✦")

    override fun initialize() {
        on<TickEvent> {
            val player = minecraft.player ?: return@on
            val speed = (player.getAttributeBaseValue(Attributes.MOVEMENT_SPEED) * 1000f + 0.5f).toInt()
            setLine("&f$speed✦")
        }

        on<RenderOverlayEvent> { event ->
            draw(event.ctx)
        }
    }
}