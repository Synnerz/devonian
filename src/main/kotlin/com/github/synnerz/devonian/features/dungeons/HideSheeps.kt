package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.events.PreExtractRenderEntityEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import net.minecraft.world.entity.animal.sheep.Sheep

object HideSheeps : Feature(
    "hideSheeps",
    "Hides any sheeps inside of dungeons",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "Hiders"
) {
    override fun initialize() {
        on<PreExtractRenderEntityEvent> { event ->
            if (event.entity !is Sheep) return@on

            event.cancel()
        }
    }
}