package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.barrl.Context
import com.github.synnerz.devonian.api.dungeon.DungeonScanner
import com.github.synnerz.devonian.api.dungeon.mapEnums.DoorTypes
import com.github.synnerz.devonian.api.dungeon.mapEnums.RoomTypes
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.features.Feature
import kotlinx.atomicfu.atomic
import java.awt.Color

object BoxDoors : Feature(
    "boxDoors",
    "Draws boxes around doors.",
    "Dungeons",
    "catacombs"
) {
    private val SETTING_DOOR_NORMAL_WIRE_COLOR = Color(0, 128, 128, 255)
    private val SETTING_DOOR_NORMAL_FILL_COLOR = Color(0, 128, 128, 0)
    private val SETTING_DOOR_LOCKED_WIRE_COLOR = Color(255, 0, 0, 255)
    private val SETTING_DOOR_LOCKED_FILL_COLOR = Color(255, 0, 0, 64)
    private val SETTING_DOOR_KEY_WIRE_COLOR = Color(0, 255, 0, 255)
    private val SETTING_DOOR_KEY_FILL_COLOR = Color(0, 255, 0, 64)
    private val settingRenderNormalDoors = addSwitch(
        "renderNormalDoors",
        "Highlights normal doorways not only the wither/blood ones",
        "Highlight Normal Doors"
    )
    private val settingRenderUnknownDoors = addSwitch(
        "renderUnknownDoors",
        "Whether to highlight the doors that are in rooms that have not been explored yet",
        "Highlight Unknown Doors"
    )
    private val settingDoorLineWidth = addSlider(
        "doorLineWidth",
        "Line width of the box outline of the door",
        1.0, 10.0,
        "Door Line Width"
    )

    private val witherKeys = atomic(0)
    private val bloodKey = atomic(false)

    private val witherKeyRegex = "^.+?(\\w+) has obtained Wither Key!$".toRegex()
    private val bloodKeyRegex = "^.+?(\\w+) has obtained Blood Key!$".toRegex()
    private val witherDoorRegex = "^(\\w+) opened a WITHER door!$".toRegex()

    override fun initialize() {
        on<ChatEvent> { event ->
            var b = true
            when (event.message) {
                "A Wither Key was picked up!" -> witherKeys.incrementAndGet()
                "A Blood Key was picked up!" -> bloodKey.value = true
                "The BLOOD DOOR has been opened!" -> bloodKey.value = false
                else -> b = false
            }
            if (b) return@on

            var match = event.matches(witherKeyRegex)
            if (match != null) {
                witherKeys.incrementAndGet()
                return@on
            }
            match = event.matches(bloodKeyRegex)
            if (match != null) {
                bloodKey.value = true
                return@on
            }
            match = event.matches(witherDoorRegex)
            if (match != null) {
                witherKeys.decrementAndGet()
                return@on
            }
        }

        on<RenderWorldEvent> {
            DungeonScanner.doors.forEach {
                if (it == null) return@forEach
                val type = when (it.type) {
                    DoorTypes.NORMAL,
                    DoorTypes.ENTRANCE
                        -> {
                        if (it.rooms.any { it.type == RoomTypes.FAIRY && !it.explored }) {
                            if (witherKeys.value > 0) 1 else 2
                        } else {
                            if (!settingRenderNormalDoors.get()) return@forEach
                            0
                        }
                    }

                    DoorTypes.WITHER -> if (witherKeys.value > 0) 1 else 2
                    DoorTypes.BLOOD -> if (bloodKey.value) 1 else 2
                }

                if (type > 0 && it.opened && !it.holyShitFairyDoorPleaseStopFlashingSobs) return@forEach

                if (!settingRenderUnknownDoors.get() && it.rooms.all { !it.explored }) return@forEach

                val colorWire: Color
                val colorFill: Color
                when (type) {
                    0 -> {
                        colorWire = SETTING_DOOR_NORMAL_WIRE_COLOR
                        colorFill = SETTING_DOOR_NORMAL_FILL_COLOR
                    }

                    1 -> {
                        colorWire = SETTING_DOOR_KEY_WIRE_COLOR
                        colorFill = SETTING_DOOR_KEY_FILL_COLOR
                    }

                    2 -> {
                        colorWire = SETTING_DOOR_LOCKED_WIRE_COLOR
                        colorFill = SETTING_DOOR_LOCKED_FILL_COLOR
                    }

                    else -> return@forEach
                }

                val comp = it.comp

                Context.Immediate?.renderBox(
                    comp.wx - 1.5 + 0.5, 69.0, comp.wz - 1.5 + 0.5,
                    3.0, 4.0,
                    colorWire,
                    true, true,
                    settingDoorLineWidth.get()
                )
                Context.Immediate?.renderFilledBox(
                    comp.wx - 1.5 + 0.5, 69.0, comp.wz - 1.5 + 0.5,
                    3.0, 4.0,
                    colorFill,
                    false, true
                )
            }
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        witherKeys.value = 0
        bloodKey.value = false
    }
}