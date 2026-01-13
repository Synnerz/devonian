package com.github.synnerz.devonian.api.dungeon

import com.github.synnerz.devonian.api.dungeon.Dungeons.DungeonBoss
import com.github.synnerz.devonian.api.events.CancellableEvent
import com.github.synnerz.devonian.api.events.Event
import com.github.synnerz.devonian.api.events.Threaded

abstract class DungeonEvent {
    class MimicKilled : Event()
    @Threaded class PrinceKilled : Event()
    @Threaded class RunStarted : Event()
    // class WitherKeyDrop : Event() // maybe?
    // class BloodKeyDrop : Event() // maybe?
    class RoomEnter(val room: DungeonRoom, val idx: Int) : Event()
    class RoomLeave(val room: DungeonRoom?, val idx: Int) : Event()
    class BossRoomEnter(val boss: DungeonBoss, val floor: FloorType) : Event()
    @Threaded class FloorEnter(val floorType: FloorType) : Event()
    @Threaded class BossMessageEvent(val boss: DungeonBoss, val message: String) : Event()
    class SecretClicked(val x: Double, val y: Double, val z: Double, val isSkull: Boolean = false, val isRedstone: Boolean = false) : Event() {
        companion object {
            @JvmStatic
            val SECRET_SKULLS = listOf("e0f3e929-869e-3dca-9504-54c666ee6f23", "fed95410-aba1-39df-9b95-1d4f361eb66e")
            @JvmStatic
            val SECRET_BLOCKS = listOf("minecraft:chest", "minecraft:lever", "minecraft:trapped_chest")

            @JvmStatic
            fun isRedstonekey(id: String): Boolean = id == SECRET_SKULLS[1]
        }
    }
    class SecretPickup(val x: Double, val y: Double, val z: Double) : Event() {
        companion object {
            @JvmStatic
            val SECRET_ITEMS = setOf(
                "POTION",
                "DUNGEON_DECOY", "INFLATABLE_JERRY", "SPIRIT_LEAP", "DUNGEON_TRAP",
                "TRAINING_WEIGHTS", "DEFUSE_KIT", "DUNGEON_CHEST_KEY",
                "TREASURE_TALISMAN", "REVIVE_STONE", "ARCHITECT_FIRST_DRAFT",
                "CANDYCOMB"
            )
        }
    }
    @Threaded class SecretBatSound(val x: Double, val y: Double, val z: Double) : CancellableEvent() {
        companion object {
            @JvmStatic
            val SECRET_BATS = listOf("minecraft:entity.bat.death", "minecraft:entity.bat.hurt")
        }
    }
    class SecretBat(val x: Double, val y: Double, val z: Double) : Event()
}