package com.github.synnerz.devonian.features.dungeons.clear

import com.github.synnerz.devonian.api.dungeon.DungeonRoom
import com.github.synnerz.devonian.api.dungeon.DungeonScanner
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.dungeon.mapEnums.CheckmarkTypes
import com.github.synnerz.devonian.api.dungeon.mapEnums.DoorTypes
import com.github.synnerz.devonian.api.dungeon.mapEnums.RoomTypes
import com.github.synnerz.devonian.api.events.TickEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.hud.texthud.Alert
import com.github.synnerz.devonian.utils.BasicState

object CurrentRoomCleared : Feature(
    "currentRoomClearedAlert",
    "",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "Alerts",
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.Clear.isActiveState)
    }

    private val SETTING_ONLY_KEY = addSwitch(
        "onlyKey",
        false,
        "",
        "Only Show For Blood Rush",
    )
    private val SETTING_ALERT_TIME = addDecimalSlider(
        "alertTime",
        0.5,
        0.0, 2.0,
        "time in seconds",
        "Alert Time",
    )
    private val SETTING_NO_BLOOD_ALERT = addSwitch(
        "noBloodAlert",
        false,
        "",
        "No Blood Room Alert"
    )

    private var lastRoom: DungeonRoom? = null
    private var wasCleared = false

    override fun initialize() {
        on<TickEvent> {
            val room = DungeonScanner.currentRoom ?: return@on
            val isCleared = room.checkmark === CheckmarkTypes.WHITE || room.checkmark === CheckmarkTypes.GREEN
            val shouldTrigger = if (SETTING_NO_BLOOD_ALERT.get()) room.type !== RoomTypes.BLOOD else true
            if (lastRoom === room && !wasCleared && isCleared && room.type !== RoomTypes.FAIRY && shouldTrigger) {
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