package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.HypixelModApi
import com.github.synnerz.devonian.api.Location
import com.github.synnerz.devonian.api.Party
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.dungeon.FloorType
import com.github.synnerz.devonian.api.events.AreaEvent
import com.github.synnerz.devonian.api.events.ChatChannelEvent
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object AutoRequeueDungeons : Feature(
    "autoRequeueDungeons",
    "Automatically runs the /instancerequeue command at the end of a run.",
    Categories.DUNGEONS,
    subcategory = "QOL",
) {
    private val SETTING_NO_SOLO = addSwitch(
        "noSolo",
        false,
        "Stops the auto requeue from working whenever you are NOT in a party",
        "No Solo Reque"
    )
    private val catacombsFloors = listOf(
        "catacombs_floor_one",
        "catacombs_floor_two",
        "catacombs_floor_three",
        "catacombs_floor_four",
        "catacombs_floor_five",
        "catacombs_floor_six",
        "catacombs_floor_seven",
    )
    private val extraStatsRegex = "^ *> EXTRA STATS <\$".toRegex()
    private var needsDowntime = Collections.newSetFromMap<String>(ConcurrentHashMap())
    private var lastQueue = 0
    private var startedRun = false
    private var lastFloor: FloorType = FloorType.None
    private var currentParty: String? = null

    private fun requeue() {
        if (!Party.isLeader && Party.inParty || !Party.inParty && SETTING_NO_SOLO.get()) return
        if (lastQueue > 0 && lastQueue != Party.members.size) {
            lastQueue = 0
            return
        }

        val cmd = when {
            Location.area != "catacombs" ->
                "joindungeon ${if (lastFloor.masterMode) "master_" else ""}${catacombsFloors.getOrNull(lastFloor.floorNum - 1) ?: return}"
            else -> "instancerequeue"
        }
        ChatUtils.command(cmd)
        lastQueue = 0
    }

    override fun initialize() {
        Dungeons.timeElapsed.listen {
            if (it <= 0 || startedRun) return@listen

            startedRun = true
            HypixelModApi.requestPartyInfo()
        }

        on<HypixelModApi.PartyInfoPacket> {
            if (Party.partyHash != currentParty && needsDowntime.isNotEmpty()) {
                ChatUtils.sendMessage("&aDowntime has been reset.", true)
                lastFloor = FloorType.None
                needsDowntime.clear()
            }
            if (!Party.inParty) return@on
            lastQueue = Party.members.size
        }

        on<ChatChannelEvent.PartyChatEvent> { event ->
            val msg = event.userMessage.lowercase()
            if (msg == "r" || msg == "!r") {
                if (!needsDowntime.remove(event.name)) return@on

                ChatUtils.sendMessage("&a${event.name} is ready", true)
                if (needsDowntime.isEmpty()) requeue()
            } else if (msg == "dt" || msg.startsWith("!dt")) {
                if (needsDowntime.add(event.name)) {
                    ChatUtils.sendMessage("&bUser &6${event.name} &bneeds downtime", true)
                }
            }
        }

        on<ChatEvent> { event ->
            if (!extraStatsRegex.matches(event.message)) return@on

            lastFloor = Dungeons.floor
            currentParty = Party.partyHash

            if (needsDowntime.isEmpty()) requeue()
            else ChatUtils.command("pc ${needsDowntime.joinToString(", ")} needs downtime")
        }

        on<AreaEvent> { event ->
            if (event.area != "catacombs") return@on
            if (needsDowntime.isEmpty()) return@on

            ChatUtils.sendMessage("&aDowntime has been reset.", true)
            needsDowntime.clear()
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        startedRun = false
        lastQueue = 0
    }
}