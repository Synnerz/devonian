package com.github.synnerz.devonian.api.dungeon.mapEnums

enum class RoomTypes {
    NORMAL,
    PUZZLE,
    TRAP,
    YELLOW,
    BLOOD,
    FAIRY,
    RARE,
    ENTRANCE,
    UNKNOWN;

    companion object {
        fun byName(name: String) =
            RoomTypes.entries.find { it.name == name.uppercase() }
    }
}