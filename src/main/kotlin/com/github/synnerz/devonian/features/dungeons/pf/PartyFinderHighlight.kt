package com.github.synnerz.devonian.features.dungeons.pf

import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.dungeon.PartyFinderListener
import com.github.synnerz.devonian.api.events.ClientContainerCloseEvent
import com.github.synnerz.devonian.api.events.RenderSlotEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import java.awt.Color
import java.util.*

object PartyFinderHighlight : Feature(
    "partyFinderHighlight",
    "Highlights a party finder party if you meet the requirements to join it.",
    Categories.PARTY_FINDER,
    searchTags = setOf("pf"),
    subcategory = "Highlight"
) {
    private val SETTING_IGNORE_CATA_REQUIREMENT = addSwitch(
        "ignoreCataRequirement",
        false,
        "Ignores the cata level requirement.",
        "Ignore Cata Level",
    )
    private val SETTING_IGNORE_ROLE_LEVEL = addSwitch(
        "ignoreRoleLevel",
        false,
        "Ignores the class level requirement.",
        "Ignore Role Level",
    )
    private val SETTING_IGNORE_OWN_ROLE = addSwitch(
        "ignoreOwnRole",
        false,
        "Ignores your own class (if dupe class it wont be red highlight).",
        "Ignore Own Role",
    )

    private val whitelist = BooleanArray(54)
    private val blacklist = BooleanArray(54)

    override fun initialize() {
        on<PartyFinderListener.PartyFinderScannedEvent> { event ->
            Scheduler.scheduleTask {
                whitelist.fill(false)
                blacklist.fill(false)

                if (event.parties.isEmpty()) return@scheduleTask

                val ignoring = EnumSet.noneOf(PartyFinderListener.PartyFinderStatus::class.java)
                if (SETTING_IGNORE_CATA_REQUIREMENT.get()) ignoring.add(PartyFinderListener.PartyFinderStatus.LOW_CATA)
                if (SETTING_IGNORE_ROLE_LEVEL.get()) ignoring.add(PartyFinderListener.PartyFinderStatus.LOW_CLASS)
                if (SETTING_IGNORE_OWN_ROLE.get()) ignoring.add(PartyFinderListener.PartyFinderStatus.DUPE_CLASS)

                event.parties.forEach {
                    val arr = if ((it.disqualifications - ignoring).isEmpty()) whitelist else blacklist
                    if (it.idx >= arr.size) return@forEach
                    arr[it.idx] = true
                }
            }
        }

        on<ClientContainerCloseEvent> {
            whitelist.fill(false)
            blacklist.fill(false)
        }

        on<RenderSlotEvent> { event ->
            if (!PartyFinderListener.inPF) return@on
            if (event.isInventory()) return@on
            val slot = event.slot

            if (blacklist.getOrNull(slot.containerSlot) == true) {
                event.ctx.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, Color.RED.rgb)
            } else if (whitelist.getOrNull(slot.containerSlot) == true) {
                event.ctx.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, Color.GREEN.rgb)
            }
        }.prio = 30
    }
}