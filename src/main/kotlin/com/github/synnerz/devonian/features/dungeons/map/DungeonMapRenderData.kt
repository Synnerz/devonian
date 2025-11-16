package com.github.synnerz.devonian.features.dungeons.map

import com.github.synnerz.devonian.api.dungeon.DungeonDoor
import com.github.synnerz.devonian.api.dungeon.DungeonRoom

data class DungeonMapRenderData(
    val rooms: List<DungeonRoom?>,
    val doors: List<DungeonDoor?>,
    val options: DungeonMapRenderOptions
)