package com.github.synnerz.devonian.features.dungeons.map

import com.github.synnerz.devonian.hud.texthud.StylizedTextHud
import java.awt.Color

data class DungeonMapRenderOptions(
    val colors: Map<DungeonMapColors, Color>,
    val roomWidth: Double, // [0, 1]
    val doorWidth: Double, // [0, 1]
    val dungeonWidth: Int, // number of rooms
    val dungeonHeight: Int, // number of rooms
    val padding: Double,
    val border: Int,
    val checkMark: Boolean,
    val puzzleIcon: Boolean,
    val roomName: Boolean,
    val roomNameNotEFB: Boolean,
    val secretCount: Boolean,
    val puzzleName: Boolean,
    val iconSize: Double, // [0, 1]
    val iconAlignment: DungeonMapRoomInfoAlignment,
    val textSize: Double, // [0, 1]
    val textAlignment: DungeonMapRoomInfoAlignment,
    val stringShadow: StylizedTextHud.Shadow,
    val colorRoomName: Boolean,
    val renderUnknownRooms: Boolean,
    val dungeonStarted: Boolean,
    val unknownRoomsDarkenFactor: Double, // [0, 1]
)

enum class DungeonMapColors {
    Background,
    Border,

    RoomEntrance,
    RoomNormal, RoomMiniboss,
    RoomFairy, RoomBlood,
    RoomPuzzle,
    RoomTrap,
    RoomYellow,
    RoomRare,
    RoomUnknown,

    DoorEntrance,
    DoorWither,
    DoorBlood;
}

enum class DungeonMapRoomInfoAlignment(val str: String) {
    // affects which "cell", not sub-cell position
    TopLeft("Top Left"), TopRight("Top Right"),
    BottomLeft("Bottom Left"), BottomRight("Bottom Right"),
    Center("Center");

    companion object {
        fun from(name: String) = entries.find { it.str == name } ?: Center
    }
}