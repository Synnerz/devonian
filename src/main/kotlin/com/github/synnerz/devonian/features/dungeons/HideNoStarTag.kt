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
    private val blazeHealthRegex = "^\\[Lv15] . Blaze [\\d,]+/([\\d,]+)❤$".toRegex()
    private val noStarTagRegex = "^(?:\\[Lv\\d+] )?[༕☠♃⛏✰\uD83E\uDDB4]* ?[A-Za-z ]+ [\\dkM.,/]+❤$".toRegex()

    override fun initialize() {
        on<PacketNameChangeEvent> { event ->
            if (event.type !== EntityType.ARMOR_STAND) return@on

            val world = minecraft.level ?: return@on

            val name = event.name
            if (name.matches(blazeHealthRegex) || !name.matches(noStarTagRegex)) return@on

            Scheduler.scheduleTask { world.removeEntity(event.entityId, Entity.RemovalReason.DISCARDED) }
        }
    }
}