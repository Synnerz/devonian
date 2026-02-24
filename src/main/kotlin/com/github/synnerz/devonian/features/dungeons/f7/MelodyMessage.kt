package com.github.synnerz.devonian.features.dungeons.f7

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import net.minecraft.world.item.Items

object MelodyMessage : Feature(
    "melodyMessage",
    "",
    Categories.F7,
    "catacombs",
    subcategory = "Terminals",
) {
    private val SETTING_ANNOUNCE_IN_MELODY = addSwitch(
        "announceIn",
        true,
        "Sends a party chat message saying that you are in melody terminal",
        "MelodyMessage Announce"
    )
    private val melodyRegex = "^Click the button on time!$".toRegex()
    private val melodySlots = listOf(25, 34, 43) // first slot is 16
    private var inMelody = false

    override fun initialize() {
        on<ServerContainerOpenEvent> { event ->
            val mel = melodyRegex.matches(event.titleStr)
            if (mel && !inMelody && SETTING_ANNOUNCE_IN_MELODY.get()) {
                Scheduler.scheduleTask { ChatUtils.command("pc melody terminal") }
            }
            inMelody = mel
        }

        on<ServerContainerCloseEvent> {
            inMelody = false
        }

        on<ClientContainerCloseEvent> {
            inMelody = false
        }

        on<ServerContainerSetSlotEvent> { event ->
            if (!inMelody) return@on
            val slot = event.slot
            val idx = melodySlots.indexOf(slot)
            if (idx == -1) return@on
            if (event.itemStack.item != Items.LIME_TERRACOTTA) return@on

            Scheduler.scheduleTask {
                ChatUtils.command("pc melody ${(idx + 1) * 25}%")
            }
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        inMelody = false
    }
}