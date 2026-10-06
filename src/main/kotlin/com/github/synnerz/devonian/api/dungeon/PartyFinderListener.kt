package com.github.synnerz.devonian.api.dungeon

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.utils.StringUtils
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import java.util.*

object PartyFinderListener {
    private val CURRENTLY_SELECTED_REGEX = "^Currently Selected: (Healer|Tank|Mage|Berserk|Archer)$".toRegex()
    private val TYPE_REGEX = "^Dungeon: (Master Mode )?(The Catacombs)$".toRegex()
    private val FLOOR_REGEX = "^Floor: (?:Entrance|Floor ([IV]+))$".toRegex()
    private val NOTE_REGEX = "^Note: (.*)$".toRegex()
    private val CLASS_REQ_REGEX = "^Class Level Required: (\\d+)$".toRegex()
    private val CATA_REQ_REGEX = "^Dungeon Level Required: (\\d+)$".toRegex()
    private val PARTY_MEMBER_REGEX = "^ (\\w{1,16}): (Healer|Tank|Mage|Berserk|Archer) \\((\\d+)\\)$".toRegex()
    private val CANNOT_JOIN_REGEX = "^Complete previous floor first!$".toRegex()
    private val LOW_CLASS_REGEX = "^Requires a Class at Level \\d+!$".toRegex()
    private val LOW_CATA_REGEX = "^Requires Catacombs Level \\d+!$".toRegex()
    private val CHAT_ROLE_REGEX = "^You have selected the (Healer|Tank|Mage|Berserk|Archer) Dungeon Class!$".toRegex()
    private val CHAT_REFRESH_CD_REGEX = "^Please wait a few seconds between refreshing!$".toRegex()
    private val CHAT_REFRESHING_REGEX = "^Refreshing\\.\\.\\.$".toRegex()
    private val TAB_ROLE_REGEX = "^ (Healer|Tank|Mage|Berserk|Archer) (\\d+)(?:: [\\d.,]+%)?$".toRegex()

    var currentParties = emptyList<PartyFinderData>()
        private set
    private val parties = mutableListOf<PartyFinderData>()
    var inPF = false
        private set
    var inGate = false
        private set
    var currentRole = DungeonClass.Unknown
        private set

    @Threaded class PartyFinderScannedEvent(
        val parties: List<PartyFinderData>
    ) : Event

    data class PartyFinderMember(val name: String, val role: DungeonClass, val level: Int)

    data class PartyFinderData(
        val idx: Int,
        val floor: Int,
        val isMasterMode: Boolean,
        val note: String,
        val requiredClassLevel: Int?,
        val requiredCataLevel: Int?,
        val members: List<PartyFinderMember>,
        val missingRoles: EnumSet<DungeonClass>,
        val disqualifications: EnumSet<PartyFinderStatus>,
    )

    private fun clearParties() {
        currentParties = emptyList()
        parties.clear()
    }

    private fun reset() {
        inPF = false
        inGate = false
    }

    fun initialize() {
        EventBus.on<ServerContainerOpenEvent> { event ->
            reset()
            clearParties()

            inPF = event.titleStr == "Party Finder"
            inGate = event.titleStr == "Catacombs Gate"
        }

        EventBus.on<ServerContainerCloseEvent> {
            reset()
        }

        EventBus.on<ClientContainerCloseEvent> {
            reset()
        }

        EventBus.on<WorldChangeEvent> {
            reset()
        }

        EventBus.on<ChatEvent> { event ->
            val m = event.matches(CHAT_ROLE_REGEX) ?: return@on
            val r = m.getOrNull(1) ?: return@on

            currentRole = DungeonClass.from(r)
        }

        EventBus.on<ServerContainerSetContentEvent> { event ->
            if (inGate) {
                val stack = event.items.getOrNull(45) ?: return@on
                val lore = ItemUtils.lore(stack) ?: return@on

                for (line in lore) {
                    val match = CURRENTLY_SELECTED_REGEX.matchEntire(line)?.groupValues?.drop(1) ?: continue
                    currentRole = DungeonClass.from(match[0])
                }
                return@on
            }

            if (!inPF) return@on

            clearParties()

            for (y in 1 .. 3) {
                for (x in 1 .. 7) {
                    val idx = y * 9 + x
                    val stack = event.items.getOrNull(idx) ?: break
                    parseItem(idx, stack)
                }
            }

            currentParties = parties.toList()
            PartyFinderScannedEvent(currentParties).post()
        }
    }

    private fun parseItem(slot: Int, stack: ItemStack) {
        if (stack.isEmpty) return
        if (stack.item !== Items.PLAYER_HEAD) return

        val lore = ItemUtils.lore(stack) ?: return

        var i = 0
        val typeMatch = lore.getOrNull(i++)?.let { TYPE_REGEX.matchEntire(it) } ?: return
        val floorMatch = lore.getOrNull(i++)?.let { FLOOR_REGEX.matchEntire(it) } ?: return
        val noteMatch = lore.getOrNull(i)?.let { NOTE_REGEX.matchEntire(it) }?.also { i++ }
        val classReqMatch = lore.getOrNull(i)?.let { CLASS_REQ_REGEX.matchEntire(it) }?.also { i++ }
        val cataReqMatch = lore.getOrNull(i)?.let { CATA_REQ_REGEX.matchEntire(it) }?.also { i++ }
        i += 2

        val members = mutableListOf<PartyFinderMember>()
        val missing = EnumSet.allOf(DungeonClass::class.java)
        missing.remove(DungeonClass.Unknown)
        for (j in 0 until 5) {
            val m = lore.getOrNull(i + j)?.let { PARTY_MEMBER_REGEX.matchEntire(it) } ?: break
            val (name, role, level) = m.groupValues.drop(1)
            val r = DungeonClass.from(role)
            missing.remove(r)
            members.add(PartyFinderMember(name, r, level.toIntOrNull() ?: 0))
        }
        i += 5
        i++

        val disqualifications = EnumSet.noneOf(PartyFinderStatus::class.java)
        lore.getOrNull(i)?.let {
            when {
                CANNOT_JOIN_REGEX.matches(it) -> disqualifications.add(PartyFinderStatus.CANNOT_JOIN)
                LOW_CLASS_REGEX.matches(it) -> disqualifications.add(PartyFinderStatus.LOW_CLASS)
                LOW_CATA_REGEX.matches(it) -> disqualifications.add(PartyFinderStatus.LOW_CATA)
            }
        }
        if (members.any { it.role == currentRole }) disqualifications.add(PartyFinderStatus.DUPE_CLASS)

        parties.add(
            PartyFinderData(
                slot,
                floorMatch.groupValues.getOrNull(1)?.let {
                    if (it.isEmpty()) 0
                    else StringUtils.parseRoman(it)
                } ?: 0,
                typeMatch.groupValues.getOrNull(1)?.isNotEmpty() ?: false,
                noteMatch?.groupValues?.getOrNull(1) ?: "",
                classReqMatch?.groupValues?.getOrNull(1)?.toIntOrNull(),
                cataReqMatch?.groupValues?.getOrNull(1)?.toIntOrNull(),
                members,
                missing,
                disqualifications,
            )
        )
    }

    enum class PartyFinderStatus {
        CANNOT_JOIN, // SHOULD be, hasn't completed previous floor
        LOW_CLASS,
        LOW_CATA,
        DUPE_CLASS;
    }
}