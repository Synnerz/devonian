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
    private var assigned = false

    override fun initialize() {
        on<ClientThreadServerTickEvent> {
            val stage = Stages.FirstWatcherSpawn
            if (!stage.hasFinished()) return@on

            if (!assigned) {
                val ticks = stage.getTime().tick
                val prediction = when {
                    ticks < 390 -> 22
                    ticks < 430 -> 23
                    ticks < 450 -> 25
                    ticks < 490 -> 26
                    ticks < 510 -> 28
                    ticks < 550 -> 29
                    ticks < 570 -> 31
                    ticks < 610 -> 32
                    ticks < 630 -> 34
                    ticks < 670 -> 35
                    ticks < 690 -> 37
                    ticks < 730 -> 38
                    else -> ticks * 20 + 3
                } / 0.05

                assigned = true
                if (SETTING_ESTIMATE_ALERT.get())
                    Alert.show("&c[Watcher] &aEstimate ${"%.2fs".format(prediction * 0.05)}", 1000, false)

                Scheduler.scheduleServerTask((prediction - ticks).toInt()) {
                    if (!isEnabled() || !stage.hasFinished()) return@scheduleServerTask
                    Alert.show("&c[Watcher] Kill Now", 1500, SETTING_PLAY_SOUND.get())
                }
            }
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        assigned = false
    }
}