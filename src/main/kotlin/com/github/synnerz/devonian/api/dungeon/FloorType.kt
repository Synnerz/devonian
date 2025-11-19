package com.github.synnerz.devonian.api.dungeon

import kotlin.math.max

enum class FloorType(
    val floorNum: Int,
    val masterMode: Boolean,
    val shortName: String,
    val roomsW: Int,
    val roomsH: Int,
    val longName: String = shortName,
    val requiredPercent: Double = 1.0,
    val requiredSpeed: Int = 600
) {
    None(0, false, "", 0, 0, ""),

    Entrance(0, false, "E", 4, 4, "Entrance", 0.3, 1200),

    F1(1, false, "F1", 4, 5, requiredPercent = 0.3),
    F2(2, false, "F2", 5, 5, requiredPercent = 0.4),
    F3(3, false, "F3", 5, 5, requiredPercent = 0.5),
    F4(4, false, "F4", 6, 5, requiredPercent = 0.6, requiredSpeed = 720),
    F5(5, false, "F5", 6, 6, requiredPercent = 0.7),
    F6(6, false, "F6", 6, 6, requiredPercent = 0.85, requiredSpeed = 720),
    F7(7, false, "F7", 6, 6, requiredSpeed = 840),

    M1(1, true, "M1", 4, 5, requiredSpeed = 480),
    M2(2, true, "M2", 5, 5, requiredSpeed = 480),
    M3(3, true, "M3", 5, 5, requiredSpeed = 480),
    M4(4, true, "M4", 6, 5, requiredSpeed = 480),
    M5(5, true, "M5", 6, 6, requiredSpeed = 480),
    M6(6, true, "M6", 6, 6, requiredSpeed = 480),
    M7(7, true, "M7", 6, 6, requiredSpeed = 900);

    val maxDim = max(roomsW, roomsH)

    companion object {
        fun from(name: String): FloorType = entries.find { it.shortName == name } ?: None
    }
}