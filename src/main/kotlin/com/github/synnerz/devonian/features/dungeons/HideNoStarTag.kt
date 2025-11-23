package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.PacketNameChangeEvent
import com.github.synnerz.devonian.features.Feature
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType

object HideNoStarTag : Feature(
    "hideNoStarTag",
    "Hides name tag of mobs that do not have star in their name tag",
    "Dungeons",
    "catacombs"
) {
    private val exceptions = listOf("Blaze", "King Midas")
    private val noStarTagRegex = "^(?:\\[Lv\\d+] )?\\S* ?[A-Za-z ]+ [\\dkM.,/]+❤$".toRegex()

    override fun initialize() {
        on<PacketNameChangeEvent> { event ->
            if (event.type !== EntityType.ARMOR_STAND) return@on

            val world = minecraft.level ?: return@on

            val name = event.name
            if (exceptions.any { name.contains(it) }) return@on
            if (!name.matches(noStarTagRegex)) return@on

            Scheduler.scheduleTask { world.removeEntity(event.entityId, Entity.RemovalReason.DISCARDED) }
        }
    }
}