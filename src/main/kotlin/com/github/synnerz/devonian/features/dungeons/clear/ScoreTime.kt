package com.github.synnerz.devonian.features.dungeons.clear

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.StringUtils

object ScoreTime : Feature(
    "scoreTime",
    "Sends a message displaying the current time when S/S+ is reached",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "QOL",
) {
    private var trigger270 = false
    private var trigger300 = false

    override fun initialize() {
        Dungeons.score.listen {
            if (!isEnabled()) return@listen

            if (it >= 270 && !trigger270) {
                trigger270 = true
                ChatUtils.sendMessage("&eS &breached at &a${StringUtils.formatSeconds(Dungeons.timeElapsed.value.toLong())}", true)
                return@listen
            }
            if (it < 300 || trigger300) return@listen

            ChatUtils.sendMessage("&6S+ &breached at &a${StringUtils.formatSeconds(Dungeons.timeElapsed.value.toLong())}", true)
            trigger300 = true
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        trigger270 = false
        trigger300 = false
    }
}