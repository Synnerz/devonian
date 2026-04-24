package com.github.synnerz.devonian.features.dungeons.clear

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.Party
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.dungeon.DungeonClass
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.NameChangeEvent
import com.github.synnerz.devonian.api.events.ScoreboardEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.hud.texthud.Alert
import net.minecraft.world.entity.EntityType
import kotlin.collections.component1
import kotlin.collections.component2

object PartyNotFullAlert : Feature(
    "partyNotFullAlert",
    "Shows an alert whenever the current run player count is not the same as party count",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "Alerts",
) {
    private val SETTING_PLAY_SOUND = addSwitch(
        "playSound",
        true,
        "Plays a sound whenever the alert is shown",
        "Play Sound"
    )
    private val scoreboardPlayerRegex = "^\\[([ABMHT])] (\\w{1,16}) \\[Lv\\d+]$".toRegex()
    private val startingAtRegex = "^Starting in 2 seconds\\.$".toRegex() // very small window ):
    private val playerEntities = mutableListOf<String>()
    private val teamMembers = mutableListOf<Pair<String, DungeonClass>>()

    override fun initialize() {
        on<NameChangeEvent> { event ->
            if (Dungeons.timeElapsed.value != 0 || event.type != EntityType.PLAYER) return@on
            playerEntities.add(event.name)
        }

        on<ChatEvent> { event ->
            if (event.matches(startingAtRegex) == null) return@on
            if (!Party.inParty || Party.members.size == size()) return@on

            Alert.show("&cParty is not full (${size()}/${Party.members.size})", 2000, SETTING_PLAY_SOUND.get())
            ChatUtils.sendMessage("&cParty is not full (${size()}/${Party.members.size})", true)
        }

        on<ScoreboardEvent> { event ->
            val match = event.matches(scoreboardPlayerRegex) ?: return@on
            val ( role, name ) = match
            val roleIns = DungeonClass.from(role.toCharArray().first())
            if (roleIns == DungeonClass.Unknown) return@on

            Scheduler.scheduleTask {
                if (!playerEntities.contains(name)) return@scheduleTask
                teamMembers.removeIf { it.first == name }
                teamMembers.add(name to roleIns)
            }
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        teamMembers.clear()
        playerEntities.clear()
    }

    // hopefully this only happens because of the current player
    private fun size(): Int = if (teamMembers.isEmpty()) 1 else teamMembers.size
}