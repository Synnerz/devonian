package com.github.synnerz.devonian.features.misc.hiders

import com.github.synnerz.devonian.api.events.ExtractRenderEntityEvent
import com.github.synnerz.devonian.features.Feature
import net.minecraft.world.entity.monster.Creeper

object HideCloakCreepers : Feature(
    "hideCloakCreepers",
    "hide creepers from wither cloak (they still block clicks)",
    subcategory = "Hiders",
) {
    override fun initialize() {
        on<ExtractRenderEntityEvent> { event ->
            val ent = event.entity as? Creeper ?: return@on

            if (ent.isInvisible && ent.isPowered && ent.health == 20f) event.cancel()
        }
    }
}