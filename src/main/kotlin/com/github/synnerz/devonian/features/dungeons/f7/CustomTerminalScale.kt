package com.github.synnerz.devonian.features.dungeons.f7

import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.GuiScaleEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.BasicState
import kotlin.math.roundToInt

object CustomTerminalScale : Feature(
    "customTerminalScale",
    "Changes GUI scale when inside of a terminal.",
    Categories.F7,
    "catacombs",
    subcategory = "Terminals",
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.Terminals.isActiveState)
    }

    private val SETTING_TERMINAL_SCALE = addSlider(
        "terminalScale",
        0.0,
        0.0, 5.0,
        "0 = auto",
        "Terminal Gui Scale",
    )
    private val SETTING_TERMINAL_MELODY_SCALE = addSlider(
        "terminalMelodyScale",
        0.0,
        0.0, 5.0,
        "0 = auto",
        "Terminal Melody Gui Scale",
    )

    private val validGuis = listOf(
        "Click in order!".toRegex(),
        "^Select all the (.*?) items!$".toRegex(),
        "^What starts with: '(.*?)'\\?$".toRegex(),
        "^Change all to same color!$".toRegex(),
        "^Correct all the panes!$".toRegex()
    )
    private val melodyRegex = "^Click the button on time!$".toRegex()

    override fun initialize() {
        on<GuiScaleEvent> { event ->
            val screen = event.screen
            val title = screen.title.string ?: return@on

            if (melodyRegex.matches(title)) event.setScale(SETTING_TERMINAL_MELODY_SCALE.get().roundToInt())
            else if (validGuis.any { it.matches(title) }) event.setScale(SETTING_TERMINAL_SCALE.get().roundToInt())
        }
    }
}