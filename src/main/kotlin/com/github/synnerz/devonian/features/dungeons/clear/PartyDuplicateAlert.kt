package com.github.synnerz.devonian.features.dungeons.clear

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.dungeon.DungeonClass
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.ClientThreadServerTickEvent
import com.github.synnerz.devonian.api.events.ScoreboardEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.hud.texthud.Alert
import com.github.synnerz.devonian.utils.StringUtils.clearCodes

object PartyDuplicateAlert : Feature(
    "partyDuplicateAlert",
    "Shows an alert whenever there's duplicate classes in the dungeon run",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "Alerts",
) {
    private val SETTING_SEND_MESSAGE = addSwitch(
        "sendMessage",
        true,
        "Sends a party chat message with the names of each player duplicate class",
        "Send Message"
    )
    private val SETTING_PLAY_SOUND = addSwitch(
        "playSound",
        true,
        "Plays a sound whenever the alert is shown",
        "Play Sound"
    )
    private val scoreboardPlayerRegex = "^\\[([ABMHT])] (\\w{1,16}) \\[Lv\\d+]$".toRegex()
    private val startingAtRegex = "^Starting in 4 seconds\\.$".toRegex()
    private val roleSwapRegex = "^(\\w{1,16}) selected the (Healer|Tank|Mage|Berserk|Archer) Class!$".toRegex()
    private val teamMembers = mutableMapOf<String, DungeonClass>()
    private var shouldTick = false

    override fun initialize() {
        on<ChatEvent> { event ->
            event.matches(roleSwapRegex)?.let {
                val ( name, role ) = it
                val roleIns = DungeonClass.from(role)
                if (roleIns == DungeonClass.Unknown) return@on

                teamMembers[name] = roleIns

                return@on
            }

            if (event.matches(startingAtRegex) == null) return@on
            shouldTick = true
        }

        on<ScoreboardEvent> { event ->
            val match = event.matches(scoreboardPlayerRegex) ?: return@on
            val ( role, name ) = match
            val roleIns = DungeonClass.from(role.toCharArray().first())
            if (roleIns == DungeonClass.Unknown) return@on

            Scheduler.scheduleTask {
                teamMembers[name] = roleIns
            }
        }

        on<ClientThreadServerTickEvent> {
            if (Dungeons.timeElapsed.value > 0 || !shouldTick) return@on
            val _list = mutableMapOf<DungeonClass, MutableList<String>>()
            teamMembers.forEach { (k, v) ->
                _list.getOrPut(v) { mutableListOf() }.add(k)
            }
            val dupes = _list.filter { it.value.size > 1 }
            if (dupes.isEmpty()) return@on

            val formatted = dupes.entries.joinToString(" ") { "${it.key.colorCode}${it.key.name} &c${it.value.joinToString(" ")}" }
            Alert.show("&cDupe $formatted", 2000, SETTING_PLAY_SOUND.get())
            if (SETTING_SEND_MESSAGE.get())
                ChatUtils.command("pc dupe ${formatted.clearCodes()}")
            ChatUtils.sendMessage("&cDupe Class Found $formatted", true)
            shouldTick = false
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        teamMembers.clear()
        shouldTick = false
    }
}