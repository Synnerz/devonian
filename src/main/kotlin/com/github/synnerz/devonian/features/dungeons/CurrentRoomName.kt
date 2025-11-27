package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.dungeon.DungeonScanner
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.hud.texthud.TextHudFeature

object CurrentRoomName : TextHudFeature(
    "currentRoomName",
    "Displays the current dungeon room name you are in",
    "Dungeons",
    "catacombs"
) {
    override fun initialize() {
        on<RenderOverlayEvent> {
            if (Dungeons.inBoss.value) return@on
            val currentRoom = DungeonScanner.currentRoom ?: return@on
            if (currentRoom.name != null)
                setLine("&bRoom&f: &a${currentRoom.name}")
            draw(it.ctx)
        }
    }

    override fun getEditText(): List<String> = listOf("&bRoom&f: &aEntrance")
}