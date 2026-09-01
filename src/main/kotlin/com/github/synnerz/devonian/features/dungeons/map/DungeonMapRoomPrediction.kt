package com.github.synnerz.devonian.features.dungeons.map

import com.github.synnerz.devonian.api.dungeon.ComponentPosition
import com.github.synnerz.devonian.api.dungeon.DungeonDoor
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.dungeon.FloorType
import com.github.synnerz.devonian.api.dungeon.mapEnums.DoorTypes
import com.github.synnerz.devonian.api.dungeon.mapEnums.RoomTypes
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature

object DungeonMapRoomPrediction : Feature(
    "dungeonMapRoomPrediction",
    "Predicts the type of unopened rooms left on the map.",
    Categories.DUNGEON_MAP,
    subcategory = "Behavior",
) {
    fun analyzeRooms(rooms: List<DungeonMapRoomRenderData?>, doors: List<DungeonDoor?>, floor: FloorType) {
        if (!isEnabled()) return

        val hasTrap = floor.floorNum >= 3
        val puzzleCount = Dungeons.totalPuzzles.value

        var foundTrap = false
        var foundPuzzleCount = 0
        var foundYellow = false
        var foundBlood = false
        rooms.forEach { data ->
            val room = data?.room ?: return@forEach
            if (!room.explored) return@forEach

            when (room.type) {
                RoomTypes.TRAP -> foundTrap = true
                RoomTypes.PUZZLE -> foundPuzzleCount++
                RoomTypes.YELLOW -> foundYellow = true
                RoomTypes.BLOOD -> foundBlood = true
                else -> {}
            }
        }

        val missingTypes = mutableListOf<RoomTypes>()
        if (hasTrap && !foundTrap) missingTypes.add(RoomTypes.TRAP)
        if (foundPuzzleCount < puzzleCount) missingTypes.add(RoomTypes.PUZZLE)
        if (!foundYellow) missingTypes.add(RoomTypes.YELLOW)

        val missingCount =
            (if (hasTrap && !foundTrap) 1 else 0) +
            (puzzleCount - foundPuzzleCount) +
            (if (!foundYellow) 1 else 0)

        val emptySpots = mutableListOf<ComponentPosition>()

        for (z in 0 until floor.roomsHS) {
            loop@ for (x in 0 until floor.roomsWS) {
                val compR = ComponentPosition(z * 2, x * 2)
                val room = rooms[compR.getRoomIdx()]?.room ?: continue@loop
                if (room.explored) continue@loop
                if (room.type == RoomTypes.BLOOD) continue@loop
                if (room.doors.any { it.type != DoorTypes.NORMAL }) continue@loop
                if (!room.doors.any { it.rooms.any { it.explored } }) continue@loop

                compR.getNeighbors().forEach { (neighborR, neighborD) ->
                    val room2 = rooms[neighborR.getRoomIdx()]?.room ?: continue@loop
                    if (room2.explored) return@forEach

                    if (neighborR.getNeighbors().all { (r, d) ->
                        doors[d.getDoorIdx()] == null || rooms[r.getRoomIdx()]?.room?.explored != true
                    }) continue@loop
                }

                emptySpots.add(compR)
            }
        }

        for (z in 0 until floor.roomsH) {
            for (x in floor.roomsWS until floor.roomsW) {
                val compD = ComponentPosition(z * 2, x * 2 - 1)
                if (doors[compD.getDoorIdx()] == null) continue

                val compR1 = ComponentPosition(z * 2, x * 2)
                val room1 = rooms[compR1.getRoomIdx()]?.room ?: continue
                if (!room1.explored) continue

                val compR2 = ComponentPosition(z * 2, x * 2 + 2)
                val room2 = rooms[compR2.getRoomIdx()]?.room ?: continue
                if (room2.explored) continue

                emptySpots.add(compR2)
            }
        }

        // if they ever add more floors or change map gen, scan the bottom row of rooms for empty slots

        if (emptySpots.size > missingCount && puzzleCount == 5) missingTypes.add(RoomTypes.RARE)

        emptySpots.forEach { comp ->
            val data = rooms[comp.getRoomIdx()]
            val room = data?.room ?: return@forEach
            if (DungeonMap.SETTING_RENDER_HIDDEN_ROOMS && room.type != RoomTypes.UNKNOWN) return@forEach

            data.predictedTypes = missingTypes
        }
    }
}