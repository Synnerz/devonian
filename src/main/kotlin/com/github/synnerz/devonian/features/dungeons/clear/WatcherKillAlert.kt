package com.github.synnerz.devonian.features.dungeons.clear

import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.dungeon.DungeonClass
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.dungeon.mapEnums.RoomTypes
import com.github.synnerz.devonian.api.events.ClientThreadServerTickEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.hud.texthud.Alert
import com.github.synnerz.devonian.utils.BasicState

object WatcherKillAlert : Feature(
    "watcherKillAlert",
    "Displays an alert whenever you should start killing watcher mobs for dialog skip",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "Alerts"
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(
            Stages.WatcherDialog.hasStartedState,
            Dungeons.selfClass
                .zip(SETTING_ONLY_MAGE.state) { c, s -> !s || c == DungeonClass.Mage }
                .zip(Dungeons.currentRoom) { b, r -> b || r?.type == RoomTypes.BLOOD }
        )
    }

    private val SETTING_PLAY_SOUND = addSwitch(
        "playSound",
        true,
        "Plays a sound whenever the alert is shown",
        "WatcherKillAlert Sound"
    )
    private val SETTING_ONLY_MAGE = addSwitch(
        "onlyMage",
        false,
        "Only shows the alert when playing the mage class or in the blood room.",
        "WatcherKillAlert Only Mage",
    )
    private var assigned = false

    override fun initialize() {
        on<ClientThreadServerTickEvent> {
            val stage = Stages.WatcherDialog
            if (stage.hasStarted() && !assigned) {
                val seconds = if (Dungeons.floor.floorNum == 7) 20.5 else 20.0
                assigned = true

                Scheduler.scheduleServerTask((seconds / 0.05).toInt()) {
                    if (!isEnabled() || !stage.hasStarted()) return@scheduleServerTask
                    Alert.show("&c[Watcher] Kill Now", 1500, SETTING_PLAY_SOUND.get())
                }
            }
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        assigned = false
    }
}