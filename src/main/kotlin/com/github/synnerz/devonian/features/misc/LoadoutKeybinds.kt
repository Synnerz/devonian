package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.Location
import com.github.synnerz.devonian.api.ScreenUtils
import com.github.synnerz.devonian.api.events.GuiClickEvent
import com.github.synnerz.devonian.api.events.GuiKeyDownEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.BasicState
import com.mojang.blaze3d.platform.InputConstants

object LoadoutKeybinds : Feature(
    "loadoutKeybinds",
    "Whenever inside of loadout gui it will allow you to press the set keys to switch through your loadouts",
    subcategory = "General",
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Location.stateInSkyblock)
    }

    private val SETTING_KEYBIND_NEXT = addKeybind(
        "keybindNext",
        InputConstants.KEY_D,
        "Keybind to next loadout page",
        "Next Loadout",
    )
    private val SETTING_KEYBIND_PREVIOUS = addKeybind(
        "keybindPrevious",
        InputConstants.KEY_A,
        "Keybind to previous loadout page",
        "Previous Loadout",
    )
    private val SETTING_KEYBIND_ONE = addKeybind(
        "keybind1",
        InputConstants.KEY_1,
        "",
        "1st Loadout",
    )
    private val SETTING_KEYBIND_TWO = addKeybind(
        "keybind2",
        InputConstants.KEY_2,
        "",
        "2nd Loadout",
    )
    private val SETTING_KEYBIND_THREE = addKeybind(
        "keybind3",
        InputConstants.KEY_3,
        "",
        "3rd Loadout",
    )
    private val SETTING_KEYBIND_FOUR = addKeybind(
        "keybind4",
        InputConstants.KEY_4,
        "",
        "4th Loadout",
    )
    private val SETTING_KEYBIND_FIVE = addKeybind(
        "keybind5",
        InputConstants.KEY_5,
        "",
        "5th Loadout",
    )
    private val SETTING_KEYBIND_SIX = addKeybind(
        "keybind6",
        InputConstants.KEY_6,
        "",
        "6th Loadout",
    )
    private val SETTING_KEYBIND_SEVEN = addKeybind(
        "keybind7",
        InputConstants.KEY_7,
        "",
        "7th Loadout",
    )
    private val SETTING_KEYBIND_EIGHT = addKeybind(
        "keybind8",
        InputConstants.KEY_8,
        "",
        "8th Loadout",
    )
    private val SETTING_KEYBIND_NINE = addKeybind(
        "keybind9",
        InputConstants.KEY_9,
        "",
        "9th Loadout",
    )
    private val SETTING_KEYBIND_TEN = addKeybind(
        "keybind10",
        -1,
        "",
        "10th Loadout",
    )
    private val SETTING_KEYBIND_ELEVEN = addKeybind(
        "keybind11",
        -1,
        "",
        "11th Loadout",
    )
    private val SETTING_KEYBIND_TWELVE = addKeybind(
        "keybind12",
        -1,
        "",
        "12th Loadout",
    )
    private val validSlots = listOf(
        14, 15, 16,
        23, 24, 25,
        32, 33, 34,
        41, 42, 43,
    )
    private val previousPage = 17
    private val nextPage = 44

    override fun initialize() {
        on<GuiKeyDownEvent> { event ->
            val screen = event.screen
            if (!screen.title.string.endsWith(") Loadouts")) return@on

            val key = event.key
            val slot = when {
                SETTING_KEYBIND_NEXT.onKeyPress(key) -> nextPage
                SETTING_KEYBIND_PREVIOUS.onKeyPress(key) -> previousPage
                SETTING_KEYBIND_ONE.onKeyPress(key) -> validSlots[0]
                SETTING_KEYBIND_TWO.onKeyPress(key) -> validSlots[1]
                SETTING_KEYBIND_THREE.onKeyPress(key) -> validSlots[2]
                SETTING_KEYBIND_FOUR.onKeyPress(key) -> validSlots[3]
                SETTING_KEYBIND_FIVE.onKeyPress(key) -> validSlots[4]
                SETTING_KEYBIND_SIX.onKeyPress(key) -> validSlots[5]
                SETTING_KEYBIND_SEVEN.onKeyPress(key) -> validSlots[6]
                SETTING_KEYBIND_EIGHT.onKeyPress(key) -> validSlots[7]
                SETTING_KEYBIND_NINE.onKeyPress(key) -> validSlots[8]
                SETTING_KEYBIND_TEN.onKeyPress(key) -> validSlots[9]
                SETTING_KEYBIND_ELEVEN.onKeyPress(key) -> validSlots[10]
                SETTING_KEYBIND_TWELVE.onKeyPress(key) -> validSlots[11]
                else -> return@on
            }
            if (slot !in 14..nextPage) return@on

            event.cancel()
            ScreenUtils.click(slot)
        }

        on<GuiClickEvent> { event ->
            if (!event.state) return@on
            val screen = event.screen
            if (!screen.title.string.endsWith(") Loadouts")) return@on

            val key = -100 + event.mbtn
            val slot = when {
                SETTING_KEYBIND_NEXT.onMousePress(key) -> nextPage
                SETTING_KEYBIND_PREVIOUS.onMousePress(key) -> previousPage
                SETTING_KEYBIND_ONE.onMousePress(key) -> validSlots[0]
                SETTING_KEYBIND_TWO.onMousePress(key) -> validSlots[1]
                SETTING_KEYBIND_THREE.onMousePress(key) -> validSlots[2]
                SETTING_KEYBIND_FOUR.onMousePress(key) -> validSlots[3]
                SETTING_KEYBIND_FIVE.onMousePress(key) -> validSlots[4]
                SETTING_KEYBIND_SIX.onMousePress(key) -> validSlots[5]
                SETTING_KEYBIND_SEVEN.onMousePress(key) -> validSlots[6]
                SETTING_KEYBIND_EIGHT.onMousePress(key) -> validSlots[7]
                SETTING_KEYBIND_NINE.onMousePress(key) -> validSlots[8]
                SETTING_KEYBIND_TEN.onMousePress(key) -> validSlots[9]
                SETTING_KEYBIND_ELEVEN.onMousePress(key) -> validSlots[10]
                SETTING_KEYBIND_TWELVE.onMousePress(key) -> validSlots[11]
                else -> return@on
            }
            if (slot !in 14..nextPage) return@on

            event.cancel()
            ScreenUtils.click(slot)
        }
    }
}