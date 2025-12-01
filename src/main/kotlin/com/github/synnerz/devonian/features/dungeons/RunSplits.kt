package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.api.splits.TimerSplit
import com.github.synnerz.devonian.api.splits.TimerSplitData
import com.github.synnerz.devonian.hud.texthud.TextHudFeature

object RunSplits : TextHudFeature(
    "runSplits",
    "Displays how long your party has take to complete Blood Rush, Blood Open & Boss Enter",
    "Dungeons",
    "catacombs"
) {
    private val SETTING_FORMAT_TIME_HUMAN = addSwitch(
        "formatTimeInHuman",
        "Formats the splits' time into a more human readable time rather than just second (example: 02m 03s instead of 123s)",
        "Run Splits Format Time Human"
    )
    private val SETTING_SEND_ALL_END = addSwitch(
        "sendAllOnRunEnd",
        "Sends all of the splits in chat whenever the run ends",
        "Run Splits Send All End"
    )
    private val mortStartRegex = "^\\[NPC] Mort: Here, I found this map when I first entered the dungeon\\.\$".toRegex()
    private val bloodOpenedRegex = "^\\[BOSS] The Watcher: (.+)\$".toRegex()
    private val bloodDoneRegex = "^\\[BOSS] The Watcher: You have proven yourself\\. You may pass\\.$".toRegex()
    private val firstSpawnRegex = "^\\[BOSS] The Watcher: Let's see how you can handle this\\.\$".toRegex()
    private val bossDialogs = listOf(
        "^\\[BOSS] Bonzo: Gratz for making it this far, but I'm basically unbeatable\\.$".toRegex(),
        "^\\[BOSS] Scarf: This is where the journey ends for you, Adventurers\\.$".toRegex(),
        "^\\[BOSS] The Professor: I was burdened with terrible news recently\\.\\.\\.$".toRegex(),
        "^\\[BOSS] Thorn: Welcome Adventurers! I am Thorn, the Spirit! And host of the Vegan Trials!$".toRegex(),
        "^\\[BOSS] Livid: Welcome, you've arrived right on time\\. I am Livid, the Master of Shadows\\.$".toRegex(),
        "^\\[BOSS] Sadan: So you made it all the way here\\.\\.\\. Now you wish to defy me\\? Sadan\\?!$".toRegex(),
        "^\\[BOSS] Maxor: WELL! WELL! WELL! LOOK WHO'S HERE!$".toRegex()
    )
    private val extraStatsRegex = "^ *> EXTRA STATS <\$".toRegex()
    private val mortTimer = TimerSplitData(null, mortStartRegex, false)
    private val timerSplit = TimerSplit(
        mortTimer,
        TimerSplitData("&cBlood Opened&f: &a$1", bloodOpenedRegex),
        TimerSplitData("&cBlood Done&f: &a$1", bloodDoneRegex)
            .addChild(
                TimerSplitData(
                    " &7(&cFirst Spawn&f: &a$1&7)",
                    firstSpawnRegex,
                    chat = "&cFirst Spawn&f: &a$1"
                )
            ),
        TimerSplitData("&bBoss Entry&f: &a$1", bossDialogs, boundTo = mortTimer)
    )

    override fun initialize() {
        on<ChatEvent> { event ->
            timerSplit.onChat(event, SETTING_FORMAT_TIME_HUMAN.get())
            event.matches(extraStatsRegex)?.let {
                if (!SETTING_SEND_ALL_END.get()) return@let
                Scheduler.scheduleServerTask(2) {
                    timerSplit.sendChat(SETTING_FORMAT_TIME_HUMAN.get())
                }
            }
        }

        on<RenderOverlayEvent> { event ->
            if (Dungeons.inBoss.value) return@on
            setLines(timerSplit.str(SETTING_FORMAT_TIME_HUMAN.get()))
            draw(event.ctx)
        }
    }

    override fun getEditText(): List<String> = timerSplit.defaultStr(SETTING_FORMAT_TIME_HUMAN.get())

    override fun onWorldChange(event: WorldChangeEvent) {
        timerSplit.reset()
    }
}