package com.github.synnerz.devonian.api.dungeon

import com.github.synnerz.devonian.api.WorldUtils
import com.github.synnerz.devonian.api.dungeon.mapEnums.DoorTypes
import net.minecraft.world.level.block.Blocks

class DungeonDoor(val comp: WorldComponentPosition) {
    var rotation: Int = -1
    var opened = false
    var type: DoorTypes = DoorTypes.NORMAL
    val rooms = mutableSetOf<DungeonRoom>()
    val roomComp1: ComponentPosition
    val roomComp2: ComponentPosition

    init {
        val cx = comp.cx
        val cz = comp.cz
        if ((cx and 1) == 1) {
            roomComp1 = ComponentPosition((cx - 1) shr 1, cz shr 1)
            roomComp2 = ComponentPosition((cx + 1) shr 1, cz shr 1)
        } else {
            roomComp1 = ComponentPosition(cx shr 1, (cz - 1) shr 1)
            roomComp2 = ComponentPosition(cx shr 1, (cz + 1) shr 1)
        }
    }

    override fun toString(): String {
        return "DungeonDoor[type=\"$type\", rotation=\"$rotation\", opened=\"$opened\"]"
    }

    fun check() {
        if (opened) return
        val x = comp.wx
        val z = comp.wz
        if (!WorldUtils.isChunkLoaded(x, z)) return

        val blockId = WorldUtils.getBlockState(x, 69, z) ?: return

        opened = blockId.isAir || blockId.block == Blocks.BARRIER

        type = when (blockId.block) {
            // idk anything infested probably just to be safe
            Blocks.INFESTED_COBBLESTONE,
            Blocks.INFESTED_CHISELED_STONE_BRICKS,
            Blocks.INFESTED_CRACKED_STONE_BRICKS,
            Blocks.INFESTED_DEEPSLATE,
            Blocks.INFESTED_MOSSY_STONE_BRICKS,
            Blocks.INFESTED_STONE,
            Blocks.INFESTED_STONE_BRICKS
                -> DoorTypes.ENTRANCE

            Blocks.COAL_BLOCK -> DoorTypes.WITHER
            Blocks.RED_TERRACOTTA -> DoorTypes.BLOOD
            else -> DoorTypes.NORMAL
        }
    }
}