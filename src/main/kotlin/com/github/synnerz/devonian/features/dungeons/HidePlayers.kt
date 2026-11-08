package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.Location
import com.github.synnerz.devonian.api.events.PreExtractRenderEntityEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.api.kuudra.KuudraEvents
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import net.minecraft.world.entity.player.Player

object HidePlayers : Feature(
    "hidePlayers",
    "Hides players in a specified radius so they cannot be rendered near you",
    Categories.DUNGEONS,
    subcategory = "Hiders"
) {
    private val SETTING_CANCEL_RADIUS = addSlider(
        "radius",
        1.0,
        1.0, 10.0,
        "The radius at which the players will not get rendered",
        "HidePlayers Radius"
    )
    private val SETTING_IN_KUUDRA = addSwitch(
        "inKuudra",
        false,
        "Works in kuudra specifically phase 5",
        "In Kuudra",
    )
    private var stunned = false

    override fun initialize() {
        on<PreExtractRenderEntityEvent> { event ->
            if (Location.area == "kuudra" && (!SETTING_IN_KUUDRA.get() || !stunned)) return@on

            val entity = event.entity as? Player ?: return@on
            val player = minecraft.player ?: return@on
            if (entity.id == player.id || entity.uuid.version() != 4) return@on
            if (entity.distanceToSqr(player) > SETTING_CANCEL_RADIUS.get()) return@on

            event.cancel()
        }.setEnabled(Location.stateArea.map { it == "catacombs" || it == "kuudra" })

        on<KuudraEvents.FuelPhaseLeave> {
            stunned = true
        }.setEnabled(SETTING_IN_KUUDRA.state)
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        stunned = false
    }
}