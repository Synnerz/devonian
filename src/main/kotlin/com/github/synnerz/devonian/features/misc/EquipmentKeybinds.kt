package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.Location
import com.github.synnerz.devonian.api.ScreenUtils
import com.github.synnerz.devonian.api.events.GuiClickEvent
import com.github.synnerz.devonian.api.events.GuiKeyDownEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.BasicState
import com.mojang.blaze3d.platform.InputConstants

object EquipmentKeybinds : Feature(
    "equipmentKeybinds",
    "Whenever inside of equipment gui it will allow you to press the set keys to switch through your equipments.",
    subcategory = "General",
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Location.stateInSkyblock)
    }

    private val SETTING_KEYBIND_NEXT = addKeybind(
        "keybindNext",
        InputConstants.KEY_D,
        "Keybind to next equipment page",
        "Next Equipment",
    )
    private val SETTING_KEYBIND_PREVIOUS = addKeybind(
        "keybindPrevious",
        InputConstants.KEY_A,
        "Keybind to previous equipment page",
        "Previous Equipment",
    )
    private val SETTING_KEYBIND_ONE = addKeybind(
        "keybind1",
        InputConstants.KEY_1,
        "",
        "1st Equipment",
    )
    private val SETTING_KEYBIND_TWO = addKeybind(
        "keybind2",
        InputConstants.KEY_2,
        "",
        "2nd Equipment",
    )
    private val SETTING_KEYBIND_THREE = addKeybind(
        "keybind3",
        InputConstants.KEY_3,
        "",
        "3rd Equipment",
    )
    private val SETTING_KEYBIND_FOUR = addKeybind(
        "keybind4",
        InputConstants.KEY_4,
        "",
        "4th Equipment",
    )
    private val SETTING_KEYBIND_FIVE = addKeybind(
        "keybind5",
        InputConstants.KEY_5,
        "",
        "5th Equipment",
    )
    private val SETTING_KEYBIND_SIX = addKeybind(
        "keybind6",
        InputConstants.KEY_6,
        "",
        "6th Equipment",
    )
    private val SETTING_KEYBIND_SEVEN = addKeybind(
        "keybind7",
        InputConstants.KEY_7,
        "",
        "7th Equipment",
    )
    private val SETTING_KEYBIND_EIGHT = addKeybind(
        "keybind8",
        InputConstants.KEY_8,
        "",
        "8th Equipment",
    )
    private val SETTING_KEYBIND_NINE = addKeybind(
        "keybind9",
        InputConstants.KEY_9,
        "",
        "9th Equipment",
    )
    private val previousPage = 45
    private val nextPage = 53

    override fun initialize() {
        on<GuiKeyDownEvent> { event ->
            val screen = event.screen
            if (!screen.title.string.endsWith(") Equipment Sets")) return@on

            val key = event.key
            val slot = 36 + when {
                SETTING_KEYBIND_NEXT.onKeyPress(key) -> 17
                SETTING_KEYBIND_PREVIOUS.onKeyPress(key) -> 9
                SETTING_KEYBIND_ONE.onKeyPress(key) -> 0
                SETTING_KEYBIND_TWO.onKeyPress(key) -> 1
                SETTING_KEYBIND_THREE.onKeyPress(key) -> 2
                SETTING_KEYBIND_FOUR.onKeyPress(key) -> 3
                SETTING_KEYBIND_FIVE.onKeyPress(key) -> 4
                SETTING_KEYBIND_SIX.onKeyPress(key) -> 5
                SETTING_KEYBIND_SEVEN.onKeyPress(key) -> 6
                SETTING_KEYBIND_EIGHT.onKeyPress(key) -> 7
                SETTING_KEYBIND_NINE.onKeyPress(key) -> 8
                else -> return@on
            }
            if (slot !in 36..nextPage) return@on

            event.cancel()
            ScreenUtils.click(slot)
        }

        on<GuiClickEvent> { event ->
            if (!event.state) return@on

            val screen = event.screen
            if (!screen.title.string.endsWith(") Equipment Sets")) return@on

            val key = -100 + event.mbtn
            val slot = 36 + when {
                SETTING_KEYBIND_NEXT.onMousePress(key) -> 17
                SETTING_KEYBIND_PREVIOUS.onMousePress(key) -> 9
                SETTING_KEYBIND_ONE.onMousePress(key) -> 0
                SETTING_KEYBIND_TWO.onMousePress(key) -> 1
                SETTING_KEYBIND_THREE.onMousePress(key) -> 2
                SETTING_KEYBIND_FOUR.onMousePress(key) -> 3
                SETTING_KEYBIND_FIVE.onMousePress(key) -> 4
                SETTING_KEYBIND_SIX.onMousePress(key) -> 5
                SETTING_KEYBIND_SEVEN.onMousePress(key) -> 6
                SETTING_KEYBIND_EIGHT.onMousePress(key) -> 7
                SETTING_KEYBIND_NINE.onMousePress(key) -> 8
                else -> return@on
            }
            if (slot !in 36..nextPage) return@on

            event.cancel()
            ScreenUtils.click(slot)
        }
    }
}