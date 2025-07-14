package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.events.RenderOverlayEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.hud.HudManager
import com.github.synnerz.devonian.utils.render.Render2D

object WorldAge : Feature("worldAge") {
    private val hud = HudManager.createHud("WorldAge", "&bDay&f: &610")

    override fun initialize() {
        on<RenderOverlayEvent> { event ->
            if (minecraft.world == null) return@on

            Render2D.drawString(
                event.ctx,
                "&bDay&f: &6${minecraft.world!!.timeOfDay / 24000}",
                hud.x,
                hud.y,
                hud.scale
            )
        }
    }
}