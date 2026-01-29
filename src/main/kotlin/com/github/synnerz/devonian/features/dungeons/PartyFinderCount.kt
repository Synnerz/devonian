package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.dungeon.PartyFinderListener
import com.github.synnerz.devonian.api.events.PostRenderSlotsEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature

object PartyFinderCount : Feature(
    "partyFinderCount",
    "Displays the partys' user count without having to hover over the item",
    Categories.DUNGEONS,
    subcategory = "QOL",
    searchTags = setOf("pf"),
) {
    private val parties = mutableListOf<PartyFinderListener.PartyFinderData>()

    override fun initialize() {
        on<PartyFinderListener.PartyFinderEvent> { event ->
            val p = event.parties
            if (p.isEmpty()) {
                Scheduler.scheduleTask { parties.clear() }
                return@on
            }

            Scheduler.scheduleTask {
                parties.clear()
                parties.addAll(p)
            }
        }

        on<PostRenderSlotsEvent> { event ->
            event.container.menu.slots.forEach { slot ->
                if (slot.container == minecraft.player?.inventory) return@forEach
                val data = parties.find { it.idx == slot.containerSlot } ?: return@forEach

                event.ctx.drawCenteredString(
                    minecraft.font,
                    "${data.members.size}",
                    slot.x + 14, slot.y + 8, -1
                )
            }
        }
    }
}