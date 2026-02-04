package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.events.PreExtractRenderEntityEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import net.minecraft.world.entity.player.Player

object HidePlayers : Feature(
    "hidePlayers",
    "Hides players in a specified radius so they cannot be rendered near you",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "QOL"
) {
    private val SETTING_CANCEL_RADIUS = addSlider(
        "radius",
        1.0,
        1.0, 10.0,
        "The radius at which the players will not get rendered",
        "HidePlayers Radius"
    )

    override fun initialize() {
        on<PreExtractRenderEntityEvent> { event ->
            val entity = event.entity as? Player ?: return@on
            val player = minecraft.player ?: return@on
            if (entity.id == player.id || entity.uuid.version() != 4) return@on
            if (entity.distanceToSqr(player) > SETTING_CANCEL_RADIUS.get()) return@on

            event.cancel()
        }
    }
}