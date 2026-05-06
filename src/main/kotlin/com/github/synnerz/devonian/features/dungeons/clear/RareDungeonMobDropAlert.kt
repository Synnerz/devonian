package com.github.synnerz.devonian.features.dungeons.clear

import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.dungeon.FloorType
import com.github.synnerz.devonian.api.events.NameChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.hud.texthud.Alert
import net.minecraft.world.entity.EntityTypes

object RareDungeonMobDropAlert : Feature(
    "rareDungeonMobDropAlert",
    "",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "Alerts",
) {
    private val SETTING_SM_ALL_FLOORS = addSwitch(
        "allFloors",
        false,
        "Sends an alert for a skeleton master chestplate, regardless of the floor (rather than only M7).",
        "Skele Master Alert All Floors",
    )
    private val SETTING_ALERT_TIME = addSlider(
        "time",
        3000.0,
        0.0, 5000.0,
        "",
        "Alert Time",
    )

    override fun initialize() {
        on<NameChangeEvent> { event ->
            if (event.type != EntityTypes.ARMOR_STAND) return@on

            val msg = if (event.name.contains("Ice Spray Wand")) "&bice spray :O"
                else if (
                    event.name.contains("Skeleton Master Chestplate") &&
                    (SETTING_SM_ALL_FLOORS.get() || Dungeons.floor == FloorType.M7)
                ) "&6sm cp :O"
                else return@on

            Alert.show(msg, SETTING_ALERT_TIME.get().toInt())
        }
    }
}