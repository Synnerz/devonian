package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.events.TickEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import kotlin.math.roundToInt

object CustomTerminalScale : Feature(
    "customTerminalScale",
    "Sets a different gui scale when you enter a terminal gui and re-sets it back to the original one once it's closed",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "Terminals"
) {
    private val SETTING_TERMINAL_SCALE = addSlider(
        "terminalScale",
        0.0,
        0.0, 5.0,
        "0 = auto",
        "Terminal Gui Scale"
    )
    private val SETTING_TERMINAL_MELODY_SCALE = addSlider(
        "terminalMelodyScale",
        0.0,
        0.0, 5.0,
        "0 = auto",
        "Terminal Melody Gui Scale"
    )
    private val validGuis = listOf(
        "Click in order!".toRegex(),
        "^Select all the (.*?) items!$".toRegex(),
        "^What starts with: '(.*?)'\\?$".toRegex(),
        "^Change all to same color!$".toRegex(),
        "^Correct all the panes!$".toRegex()
    )
    private val melodyRegex = "^Click the button on time!$".toRegex()
    private var oldScale = -1

    override fun initialize() {
        on<TickEvent> {
            val screen = minecraft.screen

            if (screen == null && oldScale != -1) {
                setScale(oldScale)
                oldScale = -1
                return@on
            }
            if (!Dungeons.inBoss.value || Dungeons.floor.floorNum != 7 || oldScale != -1) return@on
            val title = screen?.title?.string ?: return@on
            val guiScale = minecraft.options.guiScale()

            if (validGuis.any { it.matches(title) }) {
                oldScale = guiScale.get()
                setScale(SETTING_TERMINAL_SCALE.get().roundToInt())
                return@on
            }
            if (!melodyRegex.matches(title)) return@on

            oldScale = guiScale.get()
            setScale(SETTING_TERMINAL_MELODY_SCALE.get().roundToInt())
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        oldScale = -1
    }

    private fun setScale(scale: Int) {
        Scheduler.scheduleTask {
            minecraft.options.guiScale().set(scale)
        }
    }

    fun shouldScale(title: String): Boolean {
        return isEnabled() && (validGuis.any { it.matches(title) } || melodyRegex.matches(title))
    }
}