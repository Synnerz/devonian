package com.github.synnerz.devonian.features.dungeons.map

import java.awt.Color

data class DungeonMapRenderOptions(
    val colors: Map<DungeonMapColors, Color>,
    val roomWidth: Double, // [0, 1]
    val doorWidth: Double, // [0, 1]
    val dungeonSize: Int, // number of rooms
    val roomName: Boolean,
    val checkMark: Boolean,
    val secretCount: Boolean,
    val textSize: Double, // [0, 1]
    val textAlignment: DungeonMapRoomInfoAlignment,
    val iconSize: Double, // [0, 1]
    val iconAlignment: DungeonMapRoomInfoAlignment,
    val colorRoomName: Boolean,
    val renderUnknownRooms: Boolean,
    val dungeonStarted: Boolean,
    val unknownRoomsDarkenFactor: Double, // [0, 1]
    val puzzleIcon: Boolean,
    val puzzleName: Boolean,
    val stringShadow: Boolean,
)

enum class DungeonMapColors {
    RoomEntrance,
    RoomNormal, RoomMiniboss,
    RoomFairy, RoomBlood,
    RoomPuzzle,
    RoomTrap,
    RoomYellow,
    RoomRare,
    RoomUnknown,
    Background,

    DoorEntrance,
    DoorWither,
    DoorBlood;
}

enum class DungeonMapRoomInfoAlignment {
    // affects which "cell", not sub-cell position
    TopLeft, TopRight,
    BottomLeft, BottomRight,
    Center;
}