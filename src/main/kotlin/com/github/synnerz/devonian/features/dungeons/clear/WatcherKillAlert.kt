package com.github.synnerz.devonian.features.dungeons.clear

import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.dungeon.Stages
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
        return super.createRequirements() + listOf(Stages.FirstWatcherSpawn.hasFinishedState)
    }

    private val SETTING_PLAY_SOUND = addSwitch(
        "playSound",
        true,
        "Plays a sound whenever the alert is shown",
        "WatcherKillAlert Sound"
    )
    private val SETTING_ESTIMATE_ALERT = addSwitch(
        "estimateAlert",
        true,
        "Shows the predicted time to kill alert",
        "WatcherKillAlert Estimate"
    )
    private var killAt = 0.0

    override fun initialize() {
        on<ClientThreadServerTickEvent> {
            val stage = Stages.FirstWatcherSpawn
            if (!stage.hasFinished()) return@on

            if (killAt == 0.0) {
                val x = stage.getTime().tick * 0.05
                killAt = (((x + 2) + (3 - (x + 2) % 3)) - 1)
                if (SETTING_ESTIMATE_ALERT.get())
                    Alert.show("&c[Watcher] &aEstimate ${killAt.toInt()}s", 1000, false)

                Scheduler.scheduleServerTask(((killAt - x) / 0.05).toInt()) {
                    if (!isEnabled() || !stage.hasFinished()) return@scheduleServerTask
                    Alert.show("&c[Watcher] Kill Now", 1500, SETTING_PLAY_SOUND.get())
                }
            }
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        killAt = 0.0
    }
}