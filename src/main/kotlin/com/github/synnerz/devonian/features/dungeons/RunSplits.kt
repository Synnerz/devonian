package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.features.WorldFeature
import com.github.synnerz.devonian.hud.HudManager
import com.github.synnerz.devonian.utils.Render2D

object RunSplits : WorldFeature("runSplits", "catacombs") {
    // TODO: make a splits api for better readability
    private val hud = HudManager.createHud("runSplits", "&cBlood Opened&f: &a15s" +
            "\n&cBlood Done&f: &a15s &7(&cFirst Spawn&f: &a15s&7)" +
            "\n&bBoss Entry&f: &a15s"
    )
    private val mortStartRegex = "^\\[NPC] Mort: Here, I found this map when I first entered the dungeon\\.\$".toRegex()
    private val bloodOpenedRegex = "^\\[BOSS] The Watcher: (.+)\$".toRegex()
    private val bloodDoneRegex = "^\\[BOSS] The Watcher: You have proven yourself\\. You may pass\\.$".toRegex()
    private val firstSpawnRegex = "^\\[BOSS] The Watcher: Let's see how you can handle this\\.\$".toRegex()
    private val bossDialogs = listOf(
        "[BOSS] Bonzo: Gratz for making it this far, but I'm basically unbeatable.",
        "[BOSS] Scarf: This is where the journey ends for you, Adventurers.",
        "[BOSS] The Professor: I was burdened with terrible news recently...",
        "[BOSS] Thorn: Welcome Adventurers! I am Thorn, the Spirit! And host of the Vegan Trials!",
        "[BOSS] Livid: Welcome, you've arrived right on time. I am Livid, the Master of Shadows.",
        "[BOSS] Sadan: So you made it all the way here... Now you wish to defy me? Sadan?!",
        "[BOSS] Maxor: WELL! WELL! WELL! LOOK WHO'S HERE!"
    )
    val timePool = mutableMapOf(
        "Mort" to SplitData("Mort", "", 0L),
        "Opened" to SplitData("Opened", "Mort", 0L),
        "First" to SplitData("First", "Opened", 0L),
        "Done" to SplitData("Done", "Opened", 0L),
        "Boss" to SplitData("Boss", "Done", 0L)
    )

    data class SplitData(val name: String, val boundTo: String, var time: Long) {
        fun boundToTime(): Long? = timePool[boundTo]?.time
    }

    override fun initialize() {
        on<ChatEvent> { event ->
            if (event.matches(mortStartRegex) != null) return@on setTimeNow("Mort")
            if (event.matches(bloodOpenedRegex) != null && timePool["Opened"]?.time == 0L) {
                // TODO: whenever making the split api be sure to make this dynamic
                //  since you can just take the setTImeNow and send a message automatically
                setTimeNow("Opened")
                ChatUtils.sendMessage("&cBlood Opened&f: &a${secondsBetween("Opened")}a", true)
                return@on
            }
            if (event.matches(firstSpawnRegex) != null) {
                setTimeNow("First")
                ChatUtils.sendMessage("&cFirst Spawn&f: &a${secondsBetween("First")}a", true)
                return@on
            }
            if (event.matches(bloodDoneRegex) != null) {
                setTimeNow("Done")
                ChatUtils.sendMessage("&cBlood Done&f: &a${secondsBetween("Done")}s", true)
                return@on
            }

            if (!bossDialogs.contains(event.message)) return@on
            setTimeNow("Boss")
            ChatUtils.sendMessage("&bBoss Entry&f: &a${secondsBetween("Boss")}s", true)
        }

        on<RenderOverlayEvent> { event ->
            val str = "&cBlood Opened&f: &a${secondsBetween("Opened")}s" +
                    "\n&cBlood Done&f: &a${secondsBetween("Done")}s" +
                        " &7(&cFirst Spawn&f: &a${secondsBetween("First")}s&7)" +
                    "\n&bBoss Entry&f: &a${secondsBetween("Boss")}s"

            Render2D.drawStringNW(
                event.ctx,
                str,
                hud.x, hud.y, hud.scale
            )
        }
    }

    private fun setTimeNow(title: String) {
        timePool[title]!!.time = System.currentTimeMillis()
    }

    private fun millisBetween(splitData: SplitData?): Long {
        var first = splitData?.time
        if (first == null || first == 0L)
            first = System.currentTimeMillis()

        var second = splitData?.boundToTime()
        if (second == null || second == 0L)
            second = System.currentTimeMillis()

        return first - second
    }

    private fun secondsBetween(title: String): Long {
        val splitData = timePool[title]
        return millisBetween(splitData) / 1000
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        for (splitData in timePool)
            splitData.value.time = 0L
    }
}