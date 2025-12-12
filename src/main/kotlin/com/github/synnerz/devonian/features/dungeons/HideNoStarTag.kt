package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.NameChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType

object HideNoStarTag : Feature(
    "hideNoStarTag",
    "Hides name tag of mobs that do not have star in their name tag",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "QOL",
) {
    // "Boomer Psycho"
    private val bloodExceptions = setOf(
        "Putrid", "Reaper", "Vader", "Frost", "Cannibal", "Revoker", "Tear", "Mr. Dead", "Skull",
        "Walker", "Psycho", "Ooze", "Freak", "Flamer", "Mute", "Leech", "Parasite",
        "Bonzo", "Scarf", "Spirit Bear", "Livid",
        "L.A.S.R.", "The Diamond Giant", "Jolly Pink Giant", "Bigfoot"
    )
    private val exceptions = setOf(
        // TODO: admin souls
        "Mimic", "Prince", "Crypt Undead",
        "Blaze",
        "King Midas",
        "Deathmite"
    ) + bloodExceptions
    private val noStarTagRegex = "^(?:\\[Lv\\d+] )?\\S* ?([A-Za-z ]+) [\\dkM.,/]+❤$".toRegex()

    override fun initialize() {
        on<NameChangeEvent> { event ->
            if (event.type !== EntityType.ARMOR_STAND) return@on

            val world = minecraft.level ?: return@on

            val name = event.name
            val match = noStarTagRegex.matchEntire(name) ?: return@on
            val mobName = match.groupValues.getOrNull(1) ?: return@on
            if (exceptions.contains(mobName)) return@on

            mobName.indexOf(" ").also {
                if (it < 0) return@also
                val bloodName = mobName.substring(it + 1)
                if (bloodExceptions.contains(bloodName)) return@on
            }

            Scheduler.scheduleTask { world.removeEntity(event.entityId, Entity.RemovalReason.DISCARDED) }
        }
    }
}