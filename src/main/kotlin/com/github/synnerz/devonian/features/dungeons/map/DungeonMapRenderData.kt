package com.github.synnerz.devonian.features.dungeons.map

import com.github.synnerz.devonian.api.dungeon.DungeonDoor

data class DungeonMapRenderData(
    val rooms: List<DungeonMapRoomRenderData?>,
    val doors: List<DungeonDoor?>,
    val options: DungeonMapRenderOptions,
    val renderScale: Int,
)