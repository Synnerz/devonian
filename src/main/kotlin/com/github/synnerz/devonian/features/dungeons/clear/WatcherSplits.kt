package com.github.synnerz.devonian.features.dungeons.clear

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.ClientThreadServerTickEvent
import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.api.splits.TimeUnit
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.BasicState

object WatcherSplits : TextHudFeature(
    "watcherSplits",
    "Displays Dialog Time, Watcher Move and Blood Clear",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "HUD",
    searchTags = setOf("dungeons", "timer"),
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.Clear.isActiveState.zip(SETTING_SHOW_IN_BOSS.state, Boolean::or))
    }

    private val SETTING_FORMAT = addSelection(
        "format",
        0,
        listOf("Real Time", "Server Ticks", "Both"),
        "\"Kill In\" cannot be set in real time because its ticks prediction",
        "Time Format",
    )
    private val SETTING_SHOW_IN_BOSS = addSwitch(
        "showInBoss",
        false,
        "",
        "Show In Boss",
    )

    private var predictedTicks = -1

    override fun initialize() {
        on<ClientThreadServerTickEvent> {
            val stage = Stages.WatcherDialog
            if (!stage.hasFinished()) return@on

            if (predictedTicks == -1) {
                val ticks = stage.getTime().tick
                val guess = when {
                    ticks < 390 -> 22
                    ticks < 441 -> 23
                    ticks < 460 -> 25
                    ticks < 490 -> 26
                    ticks < 510 -> 27
                    ticks < 550 -> 29
                    ticks < 570 -> 31
                    ticks < 610 -> 32
                    ticks < 630 -> 34
                    ticks < 670 -> 35
                    ticks < 690 -> 37
                    ticks < 730 -> 38
                    else -> ticks * 20 + 3
                } / 0.05

                predictedTicks = EventBus.serverTicks() + guess.toInt() - ticks
                ChatUtils.sendMessage("&cWatcher Guess&f: &b${"%.2fs".format(guess * 0.05)}", true)
            }
        }

        on<RenderOverlayEvent> {
            setLines(buildList {
                // this stupid splits system sucks
                val stages = Stages.WatcherSplit.getSplits(TimeUnit.Format.entries[SETTING_FORMAT.get()])
                addAll(stages)
                if (stages.isEmpty()) return@buildList
                if (predictedTicks != -1) {
                    val ticks = (predictedTicks - EventBus.serverTicks()).coerceAtLeast(0)
                    add("&cKill In&f: &b${"%.2fs".format(ticks * 0.05)}")
                }
                else
                    add("&cKill In&f: &7...")
            })
            draw(it.ctx)
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        predictedTicks = -1
    }

    override fun getEditText(): List<String> = Stages.WatcherSplit.getSplits(TimeUnit.Format.Both, TimeUnit.now())
}