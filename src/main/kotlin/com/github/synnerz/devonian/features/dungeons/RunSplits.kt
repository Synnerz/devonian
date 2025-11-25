package com.github.synnerz.devonian.features.dungeons

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
    private val timerSplit = TimerSplit(
        TimerSplitData(null, mortStartRegex, false),
        TimerSplitData("&cBlood Opened&f: &a$1", bloodOpenedRegex),
        TimerSplitData("&cBlood Done&f: &a$1", bloodDoneRegex)
            .addChild(
                TimerSplitData(
                    " &7(&cFirst Spawn&f: &a$1&7)",
                    firstSpawnRegex,
                    chat = "&cFirst Spawn&f: &a$1"
                )
            ),
        TimerSplitData("&bBoss Entry&f: &a$1", bossDialogs)
    )

    init {
        children.add(timerSplit.event)
    }

    override fun initialize() {
        on<RenderOverlayEvent> { event ->
            setLines(timerSplit.str())
            draw(event.ctx)
        }
    }

    override fun getEditText(): List<String> = timerSplit.defaultStr()

    override fun onWorldChange(event: WorldChangeEvent) {
        timerSplit.reset()
    }
}