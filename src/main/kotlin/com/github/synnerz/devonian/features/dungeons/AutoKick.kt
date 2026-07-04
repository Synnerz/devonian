package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.Party
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.dungeon.DungeonsApi
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.StringUtils
import net.minecraft.world.item.Items

object AutoKick : Feature(
    "autoKick",
    "Automatically kicks a player whenever they join party finder if they do not meet certain requirements",
    Categories.PARTY_FINDER,
    subcategory = "General",
    searchTags = setOf("pf"),
) {
    private val SETTING_FLOOR = addSelection(
        "floor",
        0,
        listOf("Auto", "F1", "F2", "F3", "F4", "F5", "F6", "F7", "M1", "M2", "M3", "M4", "M5", "M6", "M7"),
        "If Auto is selected it'll attempt to find the currently selected floor",
        "Floor"
    )
    private val SETTING_MINIMUM_PB = addTimeSlider(
        "minimumPB",
        120.0,
        0.0, 1000.0,
        "Minimum Personal Best required",
        "Personal Best"
    )
    private val SETTING_MINIMUM_MP = addSlider(
        "minimumMP",
        1000.0,
        100.0, 3000.0,
        "Minimum Magical Power required",
        "Magical Power"
    )
    private val SETTING_PB_MODE = addSelection(
        "pbMode",
        1,
        listOf("S", "S+"),
        "Personal Best mode",
        "PB Mode"
    )
    private val SETTING_DYNAMIC_TIME = addSwitch(
        "dynamicTime",
        false,
        "Sets the time to whatever your current note is (1:50 gets converted to seconds)",
        "Dynamic Time"
    )
    private val SETTING_IGNORE_FLOOR = addSwitch(
        "ignoreFloor",
        false,
        "If you queue for m3 but your floor is set to f7 (and this is enabled) it'll kick people depending on their f7 pb, otherwise it will not",
        "Ignore Floor"
    )
    private val partyFinderJoinRegex = "^Party Finder > (\\w{1,16}) joined the dungeon group! \\((?:Healer|Tank|Mage|Berserk|Archer) Level \\d+\\)$".toRegex()
    private val partyFinderFloorRegex = "^Floor: Floor ([IV]+)$".toRegex()
    private val partyFinderFloorTypeRegex = "^Dungeon: (Master Mode )?The Catacombs$".toRegex()
    private val partyFinderCreateRegex = "^Party Finder > Your party has been queued in the dungeon finder!$".toRegex()
    private val partyFinderQueueingRegex = "^Queueing your party\\.\\.\\.$".toRegex()
    private val partyBuilderFloorRegex = "^Currently Selected: Floor ([IV]+)$".toRegex()
    private val partyBuilderFloorTypeRegex = "^Currently Selected: (Master Mode )?The Catacombs$".toRegex()
    private val partyBuilderNoteRegex = "^Current Note:$".toRegex()
    private val partyBuilderNoteTimeRegex = "(\\d):(\\d+)".toRegex()
    private var inPF = false
    private var inPFBuilder = false
    private var currentFloor = -1
        get() {
            val floor = SETTING_FLOOR.get()
            if (floor == 0) return field
            return if (floor > 7) floor % 7 else floor
        }
    private var isMMFloor = false
        get() {
            val floor = SETTING_FLOOR.get()
            if (floor == 0) return field
            return floor > 7
        }
    private var lastRequest = ""

    override fun initialize() {
        DungeonsApi.on { username, data -> onResponse(username, data) }

        on<ServerContainerOpenEvent> { event ->
            inPF = event.titleStr == "Party Finder"
            inPFBuilder = event.titleStr == "Group Builder"
        }

        on<ServerContainerCloseEvent> {
            inPF = false
            inPFBuilder = false
        }

        on<ClientContainerCloseEvent> {
            inPF = false
            inPFBuilder = false
        }

        on<ServerContainerSetSlotEvent> { event ->
            if (inPFBuilder) {
                onBuildingParty(event)
                return@on
            }

            if (!inPF) return@on
            val slot = event.slot
            val itemStack = event.itemStack
            if (slot != 53 || itemStack.item != Items.PLAYER_HEAD) return@on

            val lore = ItemUtils.lore(itemStack) ?: return@on
            for (line in lore) {
                val matchMM = partyFinderFloorTypeRegex.matchEntire(line)
                if (matchMM != null) {
                    val ( _, mm ) = matchMM.groupValues
                    isMMFloor = mm == "Master Mode "
                    continue
                }
                val match = partyFinderFloorRegex.matchEntire(line)?.groupValues?.drop(1) ?: continue
                currentFloor = StringUtils.parseRoman(match[0])
                break
            }
        }

        on<ChatEvent> { event ->
            event.matches(partyFinderCreateRegex)?.let {
                Party.inParty = true
                return@on
            }
            event.matches(partyFinderQueueingRegex)?.let {
                Party.isLeader = true
                return@on
            }

            val match = event.matches(partyFinderJoinRegex) ?: return@on
            val username = match.firstOrNull() ?: return@on

            lastRequest = username.lowercase()
            DungeonsApi.playerOrRequest(username)?.let { onResponse(lastRequest, it) }
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        lastRequest = ""
    }

    private fun onBuildingParty(event: ServerContainerSetSlotEvent) {
        val slot = event.slot
        if (slot !in 11..13) return
        val lore = ItemUtils.lore(event.itemStack) ?: return
        val reg = when (slot) {
            11 -> partyBuilderFloorTypeRegex
            12 -> partyBuilderFloorRegex
            else -> partyBuilderNoteRegex
        }
        var hasSeenNote = false

        for (line in lore) {
            if (slot == 13) {
                if (!hasSeenNote) {
                    hasSeenNote = reg.matches(line)
                    continue
                }
                if (!SETTING_DYNAMIC_TIME.get()) break
                val ( _, mm, ss ) = partyBuilderNoteTimeRegex.find(line)?.groupValues ?: continue
                val seconds = ((mm.toIntOrNull() ?: 0) * 60) + (ss.toIntOrNull() ?: 0)
                if (seconds == 0 || seconds.toDouble() == SETTING_MINIMUM_PB.get()) break
                SETTING_MINIMUM_PB.set(seconds.toDouble())
                ChatUtils.sendMessage("&bAutoKick set minimum pb to &6${StringUtils.formatSeconds(seconds.toLong())}", true)
                break
            }
            val ( _, match ) = reg.matchEntire(line)?.groupValues ?: continue
            if (slot == 11) {
                isMMFloor = match == "Master Mode "
                break
            }
            currentFloor = StringUtils.parseRoman(match)
            break
        }
    }

    private fun onResponse(username: String, data: DungeonsApi.DungeonsApiResult) {
        if (!Party.inParty || !Party.isLeader) return
        if (!SETTING_IGNORE_FLOOR.get() && (currentFloor + if (isMMFloor) 7 else 0) == SETTING_FLOOR.get()) return
        if (lastRequest.isEmpty()) return
        if (username != lastRequest) return

        lastRequest = ""

        val pbMode = when (SETTING_PB_MODE.get()) {
            0 -> "s"
            1 -> "s_plus"
            else -> return
        }
        val pb =
            if (isMMFloor)
                data.masterPBs()[pbMode]?.get("floor_${currentFloor}_ms")
            else
                data.normalPBs()[pbMode]?.get("floor_${currentFloor}_ms")
        val seconds = pb?.toIntOrNull()?.let { it / 1000 }
        if (seconds == null) {
            ChatUtils.sendMessage("&cAutoKick could not find the pb &7(pb is likely none)", true)
            return
        }
        val mp = data.magicalPower()
        val validMP = mp >= SETTING_MINIMUM_MP.get()
        val validPB = seconds <= SETTING_MINIMUM_PB.get()
        if (validMP && validPB) return

        ChatUtils.sendMessage("&bAutoKick $username does not meet the requirements | ${StringUtils.formatSeconds(seconds.toLong())} | $mp", true)
        Scheduler.scheduleTask {
            val requirement = if (validMP) "PB ${StringUtils.formatSeconds(seconds.toLong())}" else "MP $mp"
            ChatUtils.command("pc Kick $username does not meet requirements | $requirement")
            Scheduler.scheduleTask(10) {
                ChatUtils.command("p kick $username")
            }
        }
    }
}