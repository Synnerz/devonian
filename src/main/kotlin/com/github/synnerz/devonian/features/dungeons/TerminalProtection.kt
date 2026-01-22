package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.BasicState
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource

object TerminalProtection : Feature(
    "terminalProtection",
    "Allows you to set a threshold for when you should be able to start clicking terminal slots.",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "F7",
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.Terminals.isActiveState)
    }

    private val SETTING_THRESHOLD = addSlider(
        "threshold",
        400.0,
        100.0, 700.0,
        "\"lowest current safe value is 400 - ping\" - LegendaryJG",
        "Terminal Protection Threshold",
    )
    private val PREVENTED_SOUND = SoundEvents.NOTE_BLOCK_BASS
    private val terminalGuis = listOf(
        "^Click in order!$".toRegex(),
        "^Select all the (.+?) items!$".toRegex(),
        "^What starts with: '(.+?)'\\?$".toRegex(),
        "^Change all to same color!$".toRegex(),
        "^Correct all the panes!$".toRegex(),
        "^Click the button on time!$".toRegex(),
    )
    private var terminalStart = -1L

    override fun initialize() {
        on<ServerContainerOpenEvent> { event ->
            if (terminalGuis.any { it.matches(event.titleStr) })
                terminalStart = System.currentTimeMillis()
        }

        on<ServerContainerCloseEvent> {
            terminalStart = -1L
        }

        on<ClientContainerCloseEvent> {
            terminalStart = -1L
        }

        on<GuiClickEvent> { event ->
            if (terminalStart == -1L || !event.state) return@on
            if (System.currentTimeMillis() - terminalStart > SETTING_THRESHOLD.get()) return@on

            event.cancel()
            terminalStart = -1L
            minecraft.level?.playPlayerSound(
                PREVENTED_SOUND.value(),
                SoundSource.MASTER,
                1f, 0.5f,
            )
        }
    }
}