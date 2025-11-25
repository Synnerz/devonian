package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.dungeon.DungeonRoom
import com.github.synnerz.devonian.api.dungeon.DungeonScanner
import com.github.synnerz.devonian.api.dungeon.mapEnums.CheckmarkTypes
import com.github.synnerz.devonian.api.dungeon.mapEnums.DoorTypes
import com.github.synnerz.devonian.api.dungeon.mapEnums.RoomTypes
import com.github.synnerz.devonian.api.events.TickEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.hud.texthud.Alert

object CurrentRoomCleared : Feature(
    "currentRoomClearedAlert",
    "",
    "Dungeons",
    "catacombs"
) {
    private val SETTING_ONLY_KEY = addSwitch(
        "onlyKey",
        "",
        "Only Show For Blood Rush",
        false
    )
    private val SETTING_ALERT_TIME = addDecimalSlider(
        "alertTime",
        "time in seconds",
        "Alert Time",
        0.0, 2.0,
        0.5
    )

    private var lastRoom: DungeonRoom? = null
    private var wasCleared = false

    override fun initialize() {
        on<TickEvent> {
            val room = DungeonScanner.currentRoom ?: return@on
            val isCleared = room.checkmark === CheckmarkTypes.WHITE || room.checkmark === CheckmarkTypes.GREEN
            if (lastRoom === room && !wasCleared && isCleared && room.type !== RoomTypes.FAIRY) {
                if (!SETTING_ONLY_KEY.get() || room.doors.any { it.type === DoorTypes.WITHER || it.type === DoorTypes.BLOOD }) {
                    Alert.show("&aRoom Cleared!", (SETTING_ALERT_TIME.get() * 1000.0).toInt())
                }
            }
            lastRoom = room
            wasCleared = isCleared
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        lastRoom = null
        wasCleared = false
    }
}