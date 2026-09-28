package com.github.synnerz.devonian.features.dungeons.map

import com.github.synnerz.devonian.api.dungeon.ComponentPosition
import com.github.synnerz.devonian.api.dungeon.DungeonRoom
import com.github.synnerz.devonian.api.dungeon.mapEnums.RoomTypes

class DungeonMapRoomRenderData(val room: DungeonRoom) {
    var predictedTypes: Pair<ComponentPosition, List<RoomTypes>>? = null
}