package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.hud.texthud.Alert

object ScoreAlert : Feature(
    "scoreAlert",
    "Alerts on 270/300 score",
    "Dungeons",
    "catacombs"
) {
    private val SETTING_270 = addSwitch(
        "270",
        "",
        "270 Score Alert",
        true
    )
    private val SETTING_270_MESSAGE = addTextInput(
        "270message",
        "",
        "270 Score Alert Message",
        "270 Score"
    )
    private val SETTING_300 = addSwitch(
        "300",
        "",
        "300 Score Alert",
        true
    )
    private val SETTING_300_MESSAGE = addTextInput(
        "300message",
        "",
        "300 Score Alert Message",
        "300 Score"
    )

    private var sent270 = false
    private var sent300 = false

    override fun initialize() {
        Dungeons.score.listen {
            if (!isEnabled()) return@listen
            if (it >= 270 && !sent270) {
                sent270 = true
                if (SETTING_270.get()) Alert.show(SETTING_270_MESSAGE.get(), 1000)
            }
            if (it >= 300 && !sent300) {
                sent300 = true
                if (SETTING_300.get()) Alert.show(SETTING_300_MESSAGE.get(), 1000)
            }
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        sent270 = false
        sent300 = false
    }
}