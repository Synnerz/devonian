package com.github.synnerz.devonian.features.dungeons.clear

import com.github.synnerz.devonian.api.dungeon.DungeonScanner
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.TickEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.BasicState

object CurrentRoomName : TextHudFeature(
    "currentRoomName",
    "Displays the current dungeon room name you are in",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "Alerts",
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.Clear.hasFinishedState.map(Boolean::not))
    }

    override fun initialize() {
        on<TickEvent> {
            val currentRoom = DungeonScanner.currentRoom ?: return@on
            setLine("&bRoom&f: &a${currentRoom.name ?: "Unknown"}")
        }

        on<RenderOverlayEvent> {
            draw(it.ctx)
        }
    }

    override fun getEditText(): List<String> = listOf("&bRoom&f: &aEntrance")
}