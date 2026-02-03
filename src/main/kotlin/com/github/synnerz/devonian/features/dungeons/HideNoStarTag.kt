package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.NameChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType

object HideNoStarTag : Feature(
    "hideNoStarTag",
    "Hides name tag of mobs that do not have star in their name tag.",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "Hiders",
) {
    // "Boomer Psycho"
    private val bloodExceptions = setOf(
        "Putrid", "Reaper", "Vader", "Frost", "Cannibal", "Revoker", "Tear", "Mr. Dead", "Skull",
        "Walker", "Psycho", "Ooze", "Freak", "Flamer", "Mute", "Leech", "Parasite",
        "Bonzo", "Scarf", "Spirit Bear", "Livid",
        "L.A.S.R.", "The Diamond Giant", "Jolly Pink Giant", "Bigfoot"
    )
    private val exceptions = setOf(
        "Mimic", "Crypt Undead",

        "Prince Alexander",
        "Prince Bernhard",
        "Prince Christian",
        "Prince Friedrich",
        "Prince Marius",
        "Prince Nicholas",
        "Prince Pieter",
        "Prince Valentin",

        "hypixel",
        "sfarnham",
        "aPunch",
        "Jayavarmen",
        "Don Pireso",
        "codename_B",
        "WilliamTiger",
        "TheMGRF",
        "Ob111",
        "Sylent",
        "Bloozing",
        "Nitroholic",
        "Minikloon",
        "Relenter",
        "Externalizable",
        "Plancke",
        "ChiLynn",
        "skyerzz",
        "Magicboys",
        "Cecer",
        "Likaos",
        "Linfoot",
        "Dctr",
        "_onah",
        "falloutowns",
        "LadyBleu",
        "Revengeee",
        "Bembo",
        "flameboy101",
        "JamieTheGeek",
        "Judg3",
        "Rezzus",
        "AgentK",
        "Thorlon",
        "Vinny",
        "fudgiethewhale",
        "DistrictGecko",
        "Dueces",
        "Cheesey",
        "BlocksKey",
        "DEADORKAI",
        "Plummel",
        "AdamWho",
        "Winghide",
        "MistressEldrid",

        "Blaze",
        "King Midas",
        "Deathmite",

        "Akia", "Ilene", "Kari", "Lelani", "Steve", "Synestra", "Tyene", "Ussaea", "Yve", "Zana", "Trisha", "Nymira"
    ) + bloodExceptions
    private val noStarTagRegex = "^(?:\\[Lv\\d+] )?[^\\sA-Za-z]* ?([A-Za-z ]+) [\\dkMB.,/]+❤$".toRegex()

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

            Scheduler.scheduleAfterPacket { world.removeEntity(event.entityId, Entity.RemovalReason.DISCARDED) }
        }
    }
}