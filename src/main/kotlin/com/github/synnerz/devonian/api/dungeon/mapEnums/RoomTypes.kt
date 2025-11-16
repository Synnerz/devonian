package com.github.synnerz.devonian.api.dungeon.mapEnums

enum class RoomTypes(val prio: Int) {
    BLOOD(0),
    ENTRANCE(10),
    PUZZLE(20),
    RARE(30),
    YELLOW(40),
    TRAP(50),
    UNKNOWN(60),
    FAIRY(70),
    NORMAL(80);

    companion object {
        fun byName(name: String) =
            RoomTypes.entries.find { it.name == name.uppercase() }
    }
}