package com.github.synnerz.devonian.features.dungeons.clear

import com.github.synnerz.devonian.api.dungeon.DungeonEvent
import com.github.synnerz.devonian.api.dungeon.DungeonRoom
import com.github.synnerz.devonian.api.dungeon.DungeonScanner
import com.github.synnerz.devonian.api.dungeon.Dungeons
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
    "Sends an alert whenever the current room is cleared (gains a checkmark).",
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
        "Only In Blood Rush",
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
        "No Blood Room Alert",
    )
    private val SETTING_SECRETS_DONE = addSwitch(
        "secretsDone",
        false,
        "Displays an alert whenever the current room is green checked (only triggers whenever the room has already been white check marked)",
        "Secrets Done"
    )
    private val SETTING_NOT_IN_BR = addSwitch(
        "notOnlyKey",
        true,
        "Stops the title from showing if you are currently in a Blood Rush room",
        "Not In Blood Rush"
    )

    private var lastRoom: DungeonRoom? = null
    private var wasCleared = false

    override fun initialize() {
        on<TickEvent> {
            val room = DungeonScanner.currentRoom ?: return@on
            val isCleared = room.checkmark === CheckmarkTypes.WHITE || room.checkmark === CheckmarkTypes.GREEN
            val shouldTrigger = if (SETTING_NO_BLOOD_ALERT.get()) room.type !== RoomTypes.BLOOD else true
            if (lastRoom === room && !wasCleared && isCleared && room.type !== RoomTypes.FAIRY && shouldTrigger) {
                if (
                    (!SETTING_ONLY_KEY.get() || Dungeons.inBloodRush.value) &&
                    (!SETTING_NOT_IN_BR.get() || !Dungeons.inBloodRush.value)
                ) {
                    Alert.show("&aRoom Cleared!", (SETTING_ALERT_TIME.get() * 1000.0).toInt())
                }
            }
            lastRoom = room
            wasCleared = isCleared
        }

        on<DungeonEvent.RoomUpdateEvent> { event ->
            if (!SETTING_SECRETS_DONE.get()) return@on
            if (
                event.clientSide ||
                event.previousCheck != CheckmarkTypes.WHITE ||
                event.currentCheck != CheckmarkTypes.GREEN
            ) return@on
            val currentRoom = DungeonScanner.currentRoom ?: return@on
            val room = event.room
            if (room != currentRoom) return@on
            if (room.totalSecrets < 1) return@on
            if (Dungeons.inBloodRush.value && SETTING_NOT_IN_BR.get()) return@on

            Alert.show("&bSecrets Done!", (SETTING_ALERT_TIME.get() * 1000.0).toInt())
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        lastRoom = null
        wasCleared = false
    }
}