package com.github.synnerz.devonian.features.dungeons.m7

import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.hud.texthud.Alert
import com.github.synnerz.devonian.utils.BasicState

object DragonSpawnAlert : Feature(
    "dragonSpawnAlert",
    "alerts with dragon spawning",
    Categories.M7,
    "catacombs",
    searchTags = setOf("priority", "dragprio"),
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.WitherKing.isActiveState)
    }

    private val SETTING_ALERT_TIME = addSlider(
        "duration",
        1000.0,
        0.0, 5000.0,
        "",
        "Alert Duration",
    )
    private val SETTING_SOUND = addSwitch(
        "sound",
        true,
        "",
        "Alert Sound",
    )

    override fun initialize() {
        on<M7Events.ActiveDragonChanged> { event ->
            val drag = event.dragon.type

            Alert.show(
                "&l${drag.textColor}${drag.name}${if (event.dragon.isHigh) " HIGH" else ""}",
                SETTING_ALERT_TIME.get().toInt(),
                SETTING_SOUND.get()
            )
        }
    }
}