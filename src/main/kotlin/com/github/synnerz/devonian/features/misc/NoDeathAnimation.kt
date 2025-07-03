package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.events.EntityDeathEvent
import com.github.synnerz.devonian.features.Feature
import net.minecraft.entity.Entity

object NoDeathAnimation : Feature("noDeathAnimation") {
    private val lividNameRegex = "^\\w+ Livid\$".toRegex()

    override fun initialize() {
        // TODO: this might be heavy UAYOR (?)
        on<EntityDeathEvent> { event ->
            val entity = event.entity
            val world = event.world
            val name = entity.name?.string
            if (name !== null && lividNameRegex.matches(name)) return@on

            world.removeEntity(entity.id, Entity.RemovalReason.DISCARDED)
        }
    }
}