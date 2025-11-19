package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.barrl.Context
import com.github.synnerz.devonian.api.dungeon.DungeonScanner
import com.github.synnerz.devonian.api.dungeon.mapEnums.DoorTypes
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
    private const val SETTING_RENDER_UNKNOWN_DOORS = false
    private const val SETTING_RENDER_NORMAL_DOORS = true

    private val witherKeys = atomic(0)
    private val bloodKey = atomic(false)

    private val witherKeyRegex = "^.+?(\\w+) has obtained Wither Key!$".toRegex()
    private val bloodKeyRegex = "^.+?(\\w+) has obtained Blood Key!$".toRegex()
    private val witherDoorRegex = "^(\\w+) opened a WITHER door!$".toRegex()
    private val bloodDoorRegex = "^The BLOOD DOOR has been opened!$".toRegex()

    override fun initialize() {
        on<ChatEvent> { event ->
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
            match = event.matches(bloodDoorRegex)
            if (match != null) {
                bloodKey.value = false
            }
        }

        on<RenderWorldEvent> {
            DungeonScanner.doors.forEach {
                if (it == null) return@forEach
                val type = when (it.type) {
                    DoorTypes.NORMAL,
                    DoorTypes.ENTRANCE
                        -> {
                        if (!SETTING_RENDER_NORMAL_DOORS) return@forEach
                        0
                    }

                    DoorTypes.WITHER -> if (witherKeys.value > 0) 1 else 2
                    DoorTypes.BLOOD -> if (bloodKey.value) 1 else 2
                }

                if (type > 0 && it.opened) return@forEach

                if (!SETTING_RENDER_UNKNOWN_DOORS && it.rooms.all { !it.explored }) return@forEach

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
                    true, true
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