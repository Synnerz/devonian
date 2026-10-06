package com.github.synnerz.devonian.features.dungeons.pf

import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.dungeon.PartyFinderListener
import com.github.synnerz.devonian.api.events.PostRenderSlotsEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature

object PartyFinderCount : Feature(
    "partyFinderCount",
    "Displays the partys' user count without having to hover over the item",
    Categories.PARTY_FINDER,
    searchTags = setOf("pf"),
) {
    private val parties = IntArray(54)

    override fun initialize() {
        on<PartyFinderListener.PartyFinderScannedEvent> { event ->
            Scheduler.scheduleTask {
                parties.fill(0)
                event.parties.forEach {
                    if (it.idx !in parties.indices) return@forEach
                    parties[it.idx] = it.members.size
                }
            }
        }

        on<PostRenderSlotsEvent> { event ->
            if (!PartyFinderListener.inPF) return@on

            event.container.menu.slots.forEach { slot ->
                if (slot.container == minecraft.player?.inventory) return@forEach
                val count = parties.getOrNull(slot.containerSlot) ?: return@forEach
                if (count == 0) return@forEach

                event.ctx.centeredText(
                    minecraft.font,
                    "$count",
                    slot.x + 14, slot.y + 8, -1
                )
            }
        }
    }
}