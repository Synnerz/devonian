package com.github.synnerz.devonian.features.misc.inventory

import com.github.synnerz.devonian.api.Location
import com.github.synnerz.devonian.api.events.ClientThreadServerTickEvent
import com.github.synnerz.devonian.api.events.DropItemEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import java.util.*

object LimitDroppingItems : Feature(
    "limitDroppingItems",
    "prevent sending too many drop item inputs when server lags",
    Categories.VANILLA_TWEAKS,
    subcategory = "Container",
) {
    private const val MAX_DROPS = 7
    private const val TICK_SPAN = 3

    private var dropCount = 0
    private var dropHist = LinkedList<Int>().also { q ->
        repeat(TICK_SPAN) {
            q.add(0)
        }
    }

    override fun initialize() {
        on<DropItemEvent> { event ->
            if (dropCount == MAX_DROPS) event.cancel()
            else {
                dropCount++
                dropHist[0]++
            }
        }.setEnabled(Location.stateInSkyblock)

        on<ClientThreadServerTickEvent> {
            dropCount -= dropHist.removeFirst()
            dropHist.add(0)
        }
    }
}