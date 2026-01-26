package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.dungeon.PartyFinderListener
import com.github.synnerz.devonian.api.events.RenderSlotEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import java.awt.Color
import java.util.EnumSet
import java.util.concurrent.CopyOnWriteArrayList

object PartyFinderHighlight : Feature(
    "partyFinderHighlight",
    "Highlights a party finder party if you meet the requirements to join it.",
    Categories.DUNGEONS,
    subcategory = "QOL",
    searchTags = setOf("pf"),
) {
    private val SETTING_IGNORE_CATA_REQUIREMENT = addSwitch(
        "ignoreCataRequirement",
        false,
        "Ignores the cata level requirement.",
        "Party Finder Highlight Cata Level",
    )
    private val SETTING_IGNORE_ROLE_LEVEL = addSwitch(
        "ignoreRoleLevel",
        false,
        "Ignores the class level requirement.",
        "Party Finder Highlight Role Level",
    )
    private val SETTING_IGNORE_OWN_ROLE = addSwitch(
        "ignoreOwnRole",
        false,
        "Ignores your own class (if dupe class it wont be red highlight).",
        "Party Finder Highlight Role",
    )
    private val whitelist = CopyOnWriteArrayList<Boolean>()
    private val blacklist = CopyOnWriteArrayList<Boolean>()

    override fun initialize() {
        PartyFinderListener.initialize()

        on<PartyFinderListener.PartyFinderEvent> { event ->
            if (event.parties.isEmpty()) {
                blacklist.clear()
                whitelist.clear()
                return@on
            }

            val ignoring = EnumSet.noneOf(PartyFinderListener.PartyFinderStatus::class.java)
            if (SETTING_IGNORE_CATA_REQUIREMENT.get()) ignoring.add(PartyFinderListener.PartyFinderStatus.LOW_CATA)
            if (SETTING_IGNORE_ROLE_LEVEL.get()) ignoring.add(PartyFinderListener.PartyFinderStatus.LOW_ROLE)
            if (SETTING_IGNORE_OWN_ROLE.get()) ignoring.add(PartyFinderListener.PartyFinderStatus.DUPE_CLASS)

            val white = mutableListOf<Boolean>()
            val black = mutableListOf<Boolean>()
            event.parties.forEach {
                val list = if ((it.canJoin - ignoring).isEmpty()) white else black
                while (list.size <= it.idx) list.add(false)
                list[it.idx] = true
            }

            whitelist.clear()
            whitelist.addAll(white)
            blacklist.clear()
            blacklist.addAll(black)
        }

        on<RenderSlotEvent> { event ->
            if (event.isInventory()) return@on
            val slot = event.slot

            if (blacklist.getOrNull(slot.containerSlot) == true) {
                event.ctx.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, Color.RED.rgb)
            } else if (whitelist.getOrNull(slot.containerSlot) == true) {
                event.ctx.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, Color.GREEN.rgb)
            }
        }.prio = 30
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        blacklist.clear()
        whitelist.clear()
    }
}