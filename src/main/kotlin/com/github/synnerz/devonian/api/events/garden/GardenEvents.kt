package com.github.synnerz.devonian.api.events.garden

import com.github.synnerz.devonian.api.Location
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.Event
import com.github.synnerz.devonian.api.events.EventBus

object GardenEvents {
    private const val OVERCLOCKER_3000 = "Overclocker 3000"
    private val pestDropRegex = "^You received (\\d+)x ([\\w ]+) for killing an? ([\\w ]+)!$".toRegex()
    private val pestRareDropRegex = "^RARE DROP! (?:(\\d+)x )?([\\w ]+) \\(\\+[\\d,]+☘\\)\$".toRegex()

    class PestKill(val name: String) : Event
    class PestDrop(
        val name: String,
        val amount: Int,
        val isRare: Boolean = false,
    ) : Event

    fun initialize() {
        EventBus.on<ChatEvent> { event ->
            event.matches(pestDropRegex)?.let {
                val ( num, cropType, pestType ) = it
                val amount = num.toIntOrNull() ?: 0
                if (cropType != OVERCLOCKER_3000) PestKill(pestType).post()

                PestDrop(cropType, amount).post()
            }

            val rareDropMatch = event.matches(pestRareDropRegex) ?: return@on
            val ( num, cropType ) = rareDropMatch
            val amount = num.toIntOrNull() ?: 1

            PestDrop(cropType, amount, true).post()
        }.setEnabled(Location.stateInArea("garden"))
        // TODO: visitor gui enter, items etc events
    }
}