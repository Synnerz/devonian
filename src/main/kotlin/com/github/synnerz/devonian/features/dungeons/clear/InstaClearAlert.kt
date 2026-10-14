package com.github.synnerz.devonian.features.dungeons.clear

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.dungeon.DungeonEvent
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.dungeon.mapEnums.CheckmarkTypes
import com.github.synnerz.devonian.api.dungeon.mapEnums.RoomTypes
import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.hud.texthud.Alert
import net.minecraft.sounds.SoundEvents

object InstaClearAlert : Feature(
    "instaClearAlert",
    "Alerts whenever a Blood Rush room is insta cleared (meaning no Wither/Blood key drop)",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "Alerts",
) {
    private val roomChecks = mutableMapOf<String, Pair<CheckmarkTypes, Int>>()

    override fun initialize() {
        on<DungeonEvent.RoomUpdateEvent> { event ->
            val currentCheck = event.currentCheck
            val previousCheck = event.previousCheck
            val room = event.room
            val roomName = room.name ?: return@on
            if (
                room.type != RoomTypes.FAIRY &&
                (roomChecks[roomName] == null || EventBus.serverTicks() - roomChecks[roomName]!!.second < 5) &&
                (previousCheck == CheckmarkTypes.NONE || previousCheck == CheckmarkTypes.UNEXPLORED) &&
                (currentCheck == CheckmarkTypes.WHITE || currentCheck == CheckmarkTypes.GREEN) &&
                Dungeons.inBloodRush.value
            ) {
                Alert.showWithSound(
                    "&4$roomName &cinsta cleared",
                    1500,
                    SoundEvents.END_PORTAL_SPAWN,
                    1.2f,
                    1f,
                )
                ChatUtils.sendMessage("&4$roomName &cinsta cleared.", true)
            }

            roomChecks[roomName] = currentCheck to EventBus.serverTicks()
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        roomChecks.clear()
    }
}