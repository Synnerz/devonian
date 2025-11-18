package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.events.RenderEntityEvent
import com.github.synnerz.devonian.features.Feature
import net.minecraft.world.entity.LivingEntity

object NoDeathAnimation : Feature(
    "noDeathAnimation",
    "Removes the Death Animation from entities that die."
) {
    private val lividNameRegex = "^\\w+ Livid\$".toRegex()

    override fun initialize() {
        on<RenderEntityEvent> { event ->
            if (event.entity !is LivingEntity) return@on
            val entity = event.entity

            if (entity.isAlive && entity.health > 0f) return@on

            val name = entity.name?.string
            if (name != null && lividNameRegex.matches(name)) return@on

            event.ci.cancel()
        }
    }
}