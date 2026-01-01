package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.dungeon.PartyFinderListener
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import java.awt.Color
import java.util.concurrent.CopyOnWriteArrayList

object PartyFinderHighlight : Feature(
    "partyFinderHighlight",
    "Highlights a party finder party if you meet the requirements to join it",
    Categories.DUNGEONS,
    subcategory = "QOL"
) {
    private val SETTING_IGNORE_ROLE_LEVEL = addSwitch(
        "ignoreRoleLevel",
        false,
        "Ignores the class level requirement",
        "Party Finder Highlight Role Level"
    )
    private val SETTING_IGNORE_OWN_ROLE = addSwitch(
        "ignoreOwnRole",
        false,
        "Ignores your own class (if dupe class it wont be red highlight)",
        "Party Finder Highlight Role"
    )
    private val SETTING_IGNORE_CATA_REQUIREMENT = addSwitch(
        "ignoreCataRequirement",
        false,
        "Ignores the cata level requirement",
        "Party Finder Highlight Cata Level"
    )
    private val whitelist = CopyOnWriteArrayList<Int>()
    private val blacklist = CopyOnWriteArrayList<Int>()

    override fun initialize() {
        PartyFinderListener.initialize()

        on<PartyFinderListener.PartyFinderEvent> { event ->
            if (event.parties.isEmpty()) {
                blacklist.clear()
                whitelist.clear()
                return@on
            }

            event.parties.forEach {
                if (
                    it.canJoin == PartyFinderListener.PartyFinderStatus.CAN_JOIN ||
                    it.canJoin == PartyFinderListener.PartyFinderStatus.LOW_ROLE && SETTING_IGNORE_ROLE_LEVEL.get() ||
                    it.canJoin == PartyFinderListener.PartyFinderStatus.DUPE_CLASS && SETTING_IGNORE_OWN_ROLE.get() ||
                    it.canJoin == PartyFinderListener.PartyFinderStatus.LOW_CATA && SETTING_IGNORE_CATA_REQUIREMENT.get()
                ) {
                    whitelist.add(it.idx)
                    return@forEach
                }

                blacklist.add(it.idx)
            }
        }

        on<RenderSlotEvent> { event ->
            if (event.isInventory()) return@on
            val slot = event.slot

            if (blacklist.contains(slot.containerSlot)) {
                event.ctx.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, Color.RED.rgb)
                return@on
            }
            if (!whitelist.contains(slot.containerSlot)) return@on

            event.ctx.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, Color.GREEN.rgb)
        }.prio = 30
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        blacklist.clear()
        whitelist.clear()
    }
}