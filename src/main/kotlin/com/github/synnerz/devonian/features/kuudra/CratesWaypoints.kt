package com.github.synnerz.devonian.features.kuudra

import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.api.events.kuudra.KuudraEvents
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.render.Render3DImmediate
import java.awt.Color

object CratesWaypoints : Feature(
    "cratesWaypoints",
    "Adds waypoints to the crates that you need to pickup",
    Categories.KUUDRA,
    "kuudra",
    searchTags = setOf("supply", "kuudra")
) {
    private val SETTING_COLOR = addColorPicker(
        "color",
        Color(211, 0, 255).rgb,
        "The color of the waypoint",
        "Color"
    )

    override fun initialize() {
        on<RenderWorldEvent> {
            if (!KuudraEvents.inCratePhase()) return@on

            KuudraEvents.supplies().forEach {
                val pos = it.pos() ?: return@forEach

                Render3DImmediate.renderWaypoint(
                    pos.x, pos.y, pos.z,
                    SETTING_COLOR.getColor(),
                    phase = true
                )
            }
        }
    }
}