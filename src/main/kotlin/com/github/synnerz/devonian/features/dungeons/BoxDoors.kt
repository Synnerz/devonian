package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.barrl.Context
import com.github.synnerz.devonian.api.dungeon.DungeonScanner
import com.github.synnerz.devonian.api.dungeon.WorldComponentPosition
import com.github.synnerz.devonian.api.dungeon.mapEnums.CheckmarkTypes
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
    private val SETTING_DOOR_LOCKED_WIRE_COLOR = addColorPicker(
        "lockedWireColor",
        "",
        "Locked Door Outline Color",
        Color(255, 0, 0).rgb
    )
    private val SETTING_DOOR_LOCKED_FILL_COLOR = addColorPicker(
        "lockedFillColor",
        "",
        "Locked Door Fill Color",
        Color(255, 0, 0, 64).rgb
    )
    private val SETTING_DOOR_KEY_WIRE_COLOR = addColorPicker(
        "keyWireColor",
        "",
        "Unlocked Door Outline Color",
        Color(0, 255, 0).rgb
    )
    private val SETTING_DOOR_KEY_FILL_COLOR = addColorPicker(
        "keyFillColor",
        "",
        "Unlocked Door Fill Color",
        Color(0, 255, 0, 64).rgb
    )
    private val SETTING_RENDER_NORMAL_DOORS = addSwitch(
        "renderNormalDoors",
        "Highlights normal doorways not only the wither/blood ones",
        "Highlight Normal Doors",
        true
    )
    private val SETTING_HIDE_NORMAL_DOOR_GREEN = addSwitch(
        "hideNormalDoorGreen",
        "Don't box normal doors when the room it leads to already has a green checkmark",
        "Hide Useless Doors",
        true
    )
    private val SETTING_DOOR_NORMAL_WIRE_COLOR = addColorPicker(
        "normalWireColor",
        "",
        "Normal Door Outline Color",
        Color(0, 128, 128).rgb
    )
    private val SETTING_DOOR_NORMAL_FILL_COLOR = addColorPicker(
        "normalFillColor",
        "",
        "Normal Door Fill Color",
        Color(0, 128, 128, 0).rgb
    )
    private val SETTING_DOOR_LINE_WIDTH = addSlider(
        "doorLineWidth",
        "Line width of the box outline of the door",
        "Door Line Width",
        1.0, 10.0,
        3.0
    )
    private val SETTING_RENDER_HIDDEN_DOORS = addSwitch(
        "renderUnknownDoors",
        "Whether to highlight the doors that are in rooms that have not been explored yet",
        "Highlight Unknown Doors",
        false,
        cheeto = true
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
                val hasKey = when (it.type) {
                    DoorTypes.NORMAL,
                    DoorTypes.ENTRANCE
                        -> {
                        if (it.rooms.any { it.type == RoomTypes.FAIRY && !it.explored }) witherKeys.value > 0
                        else return@forEach
                    }

                    DoorTypes.WITHER -> witherKeys.value > 0
                    DoorTypes.BLOOD -> bloodKey.value
                }

                if (it.opened && !it.holyShitFairyDoorPleaseStopFlashingSobs) return@forEach

                if (!SETTING_RENDER_HIDDEN_DOORS.get() && it.rooms.all { !it.explored }) return@forEach

                val colorWire: Color
                val colorFill: Color
                if (hasKey) {
                    colorWire = SETTING_DOOR_KEY_WIRE_COLOR.getColor()
                    colorFill = SETTING_DOOR_KEY_FILL_COLOR.getColor()
                } else {
                    colorWire = SETTING_DOOR_LOCKED_WIRE_COLOR.getColor()
                    colorFill = SETTING_DOOR_LOCKED_FILL_COLOR.getColor()
                }

                drawDoor(it.comp, colorWire, colorFill)
            }

            if (SETTING_RENDER_NORMAL_DOORS.get()) {
                val room = DungeonScanner.currentRoom ?: return@on
                room.doors.forEach {
                    if (it.type != DoorTypes.NORMAL && it.type != DoorTypes.ENTRANCE) return@forEach
                    if (
                        SETTING_HIDE_NORMAL_DOOR_GREEN.get() &&
                        it.rooms.any { it !== room && it.checkmark == CheckmarkTypes.GREEN }
                    ) return@on

                    drawDoor(
                        it.comp,
                        SETTING_DOOR_NORMAL_WIRE_COLOR.getColor(),
                        SETTING_DOOR_NORMAL_FILL_COLOR.getColor()
                    )
                }
            }
        }
    }

    private fun drawDoor(comp: WorldComponentPosition, wire: Color, fill: Color) {
        Context.Immediate?.renderBox(
            comp.wx - 1.5 + 0.5, 69.0, comp.wz - 1.5 + 0.5,
            3.0, 4.0,
            wire,
            true, true,
            SETTING_DOOR_LINE_WIDTH.get()
        )
        Context.Immediate?.renderFilledBox(
            comp.wx - 1.5 + 0.5, 69.0, comp.wz - 1.5 + 0.5,
            3.0, 4.0,
            fill,
            false, true
        )
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        witherKeys.value = 0
        bloodKey.value = false
    }
}