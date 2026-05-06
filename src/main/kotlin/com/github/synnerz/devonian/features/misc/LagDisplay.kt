package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.Location
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.ServerTickEvent
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.StringUtils

object LagDisplay : TextHudFeature(
    "lagDisplay",
    "Shows how long ago the last server tick was.",
    subcategory = "General",
    searchTags = setOf("zzz"),
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Location.stateInSkyblock)
    }

    private val SETTING_THRESH = addSlider(
        "thresh",
        300.0,
        50.0, 1000.0,
        "show when server has not responded for this long",
        "Lag Threshold",
    )

    private var lastPacket = System.currentTimeMillis()

    override fun initialize() {
        on<ServerTickEvent> {
            lastPacket = System.currentTimeMillis()
        }

        on<RenderOverlayEvent> { event ->
            if (!minecraft.isMultiplayerServer) return@on
            val t = System.currentTimeMillis()
            val dt = t - lastPacket
            if (dt < SETTING_THRESH.get()) return@on

            val color = StringUtils.colorForNumber(2_000L - dt, 2_000L)
            setLine("zzz for $color%.2fs".format(dt / 1000.0))
            draw(event.ctx)
        }
    }

    override fun getEditText(): List<String> = listOf("zzz for &469.42s")
}