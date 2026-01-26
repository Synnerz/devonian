package com.github.synnerz.devonian.api.dungeon

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.ScreenUtils
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.features.dungeons.PartyFinderHighlight.minecraft
import com.github.synnerz.devonian.utils.StringUtils
import net.minecraft.world.item.Items
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

object PartyFinderListener {
    private val TYPE_REGEX = "^Dungeon: (Master Mode )?(The Catacombs)$".toRegex()
    private val FLOOR_REGEX = "^Floor: Floor ([IV]+)$".toRegex()
    private val TAB_ROLE_REGEX = "^ (Healer|Tank|Mage|Berserk|Archer) (\\d+)(?:: [\\d.,]+%)?$".toRegex()
    val USER_ROLE_REGEX = "^ (\\w{1,16}): (Healer|Tank|Mage|Berserk|Archer) \\((\\d+)\\)$".toRegex()
    private val LOW_CATA_REGEX = "^Requires Catacombs Level \\d+!$".toRegex()
    private val LOW_ROLE_REGEX = "^Requires a Class at Level \\d+!$".toRegex()
    private val CANNOT_JOIN_REGEX = "^Complete previous floor first!$".toRegex()
    private val CURRENTLY_SELECTED_REGEX = "^Currently Selected: (Healer|Tank|Mage|Berserk|Archer)$".toRegex()
    private val CHAT_ROLE_REGEX = "^You have selected the (Healer|Tank|Mage|Berserk|Archer) Dungeon Class!$".toRegex()
    private val CHAT_REFRESH_CD_REGEX = "^Please wait a few seconds between refreshing!$".toRegex()
    private val CHAT_REFRESHING_REGEX = "^Refreshing\\.\\.\\.$".toRegex()
    private val parties = CopyOnWriteArrayList<PartyFinderData>()
    private val oldParties = CopyOnWriteArrayList<PartyFinderData>()
    private var inPF = false
    private var inGate = false
    private var shouldScan = false
    private var currentRole: String? = null

    class PartyFinderEvent(
        val parties: CopyOnWriteArrayList<PartyFinderData>
    ) : Event()

    data class PartyFinderMember(val name: String, val role: String, val level: Int)

    data class PartyFinderData(
        var floor: Int = -1,
        var idx: Int,
        var isMasterMode: Boolean = false,
        val members: MutableList<PartyFinderMember> = mutableListOf(),
        // maybe we need this data later or something idk commenting for now
//        var note: String = "Empty",
//        var requiredLevel: Int = -1,
//        var requiredRoleLevel: Int = -1,
        val canJoin: EnumSet<PartyFinderStatus> = EnumSet.noneOf(PartyFinderStatus::class.java),
    )

    fun initialize() {
        EventBus.on<ServerContainerOpenEvent> { event ->
            inPF = event.titleStr == "Party Finder"
            shouldScan = inPF
            inGate = event.titleStr == "Catacombs Gate"
            if (!inPF && parties.isNotEmpty()) {
                parties.clear()
                PartyFinderEvent(parties).post()
            }
        }

        EventBus.on<ServerContainerCloseEvent> {
            if (parties.isNotEmpty()) reset()
        }

        EventBus.on<ClientContainerCloseEvent> {
            if (parties.isNotEmpty()) reset()
        }

        EventBus.on<ChatEvent> { event ->
            event.matches(CHAT_REFRESH_CD_REGEX)?.let {
                parties.clear()
                parties.addAll(oldParties)
                PartyFinderEvent(parties).post()
                return@on
            }
        }

        EventBus.on<GuiClickEvent> { event ->
            if (!inPF) return@on
            val slot = ScreenUtils.cursorSlot(event.screen) ?: return@on
            if (slot.containerSlot != 46 || slot.container == minecraft.player?.inventory) return@on

            parties.clear()
            PartyFinderEvent(parties).post()
            shouldScan = true
        }

        EventBus.on<ServerContainerSetContentEvent> { event ->
            if (inGate) {
                val starSlot = event.items.getOrNull(45) ?: return@on
                val lore = ItemUtils.lore(starSlot) ?: return@on
                for (line in lore) {
                    val match = CURRENTLY_SELECTED_REGEX.matchEntire(line)?.groupValues?.drop(1) ?: continue
                    currentRole = match[0]
                }
                inGate = false
                return@on
            }

            if (!shouldScan) return@on

            event.forEach { idx, itemStack ->
                if (itemStack == null || itemStack.item != Items.PLAYER_HEAD) return@forEach

                val lore = ItemUtils.lore(itemStack) ?: return@forEach
                var currentData: PartyFinderData? = null

                for (line in lore) {
                    if (currentData == null) {
                        val typeMatch = TYPE_REGEX.matchEntire(line)?.groupValues?.drop(1) ?: continue
                        currentData = PartyFinderData(idx = idx, isMasterMode = typeMatch[0] == "Master Mode ")
                        continue
                    }
                    if (LOW_CATA_REGEX.matches(line)) {
                        currentData.canJoin.add(PartyFinderStatus.LOW_CATA)
                        continue
                    }
                    if (LOW_ROLE_REGEX.matches(line)) {
                        currentData.canJoin.add(PartyFinderStatus.LOW_ROLE)
                        continue
                    }
                    if (CANNOT_JOIN_REGEX.matches(line)) {
                        currentData.canJoin.add(PartyFinderStatus.CANNOT_JOIN)
                        continue
                    }
                    if (currentData.floor == -1) {
                        val floorMatch = FLOOR_REGEX.matchEntire(line)?.groupValues?.drop(1) ?: continue
                        currentData.floor = StringUtils.parseRoman(floorMatch[0])
                        continue
                    }

                    val (username, role, level) = USER_ROLE_REGEX.matchEntire(line)?.groupValues?.drop(1) ?: continue

                    currentData.members.add(
                        PartyFinderMember(
                            username,
                            role,
                            level.toIntOrNull() ?: 0
                        )
                    )
                }

                if (currentData?.members?.any { it.role == currentRole } == true) {
                    currentData.canJoin.add(PartyFinderStatus.DUPE_CLASS)
                }

                if (currentData == null) return@forEach

                parties.add(currentData)
                oldParties.add(currentData)
            }

            PartyFinderEvent(parties).post()
            shouldScan = false
        }

        EventBus.on<WorldChangeEvent> {
            reset()
        }
    }

    private fun reset() {
        parties.clear()
        oldParties.clear()
        inPF = false
        shouldScan = false
        inGate = false
        currentRole = null
        PartyFinderEvent(parties).post()
    }

    enum class PartyFinderStatus {
        CANNOT_JOIN, // SHOULD be, hasn't completed previous floor
        DUPE_CLASS,
        LOW_CATA,
        LOW_ROLE,
    }
}