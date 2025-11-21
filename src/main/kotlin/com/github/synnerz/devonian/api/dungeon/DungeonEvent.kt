package com.github.synnerz.devonian.api.dungeon

import com.github.synnerz.devonian.api.dungeon.Dungeons.DungeonBoss
import com.github.synnerz.devonian.api.events.Event

abstract class DungeonEvent {
    class MimicKilled : Event()
    class PrinceKilled : Event()
    class RunStarted : Event()
    // class WitherKeyDrop : Event() // maybe?
    // class BloodKeyDrop : Event() // maybe?
    class RoomEnter(val room: DungeonRoom, val idx: Int) : Event()
    class RoomLeave(val room: DungeonRoom?, val idx: Int) : Event()
    class FloorEnter(val floorType: FloorType) : Event()
    class BossMessageEvent(val boss: DungeonBoss, val message: String) : Event()
}