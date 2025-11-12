package com.github.synnerz.devonian.api.dungeon

import com.github.synnerz.devonian.api.WorldUtils
import com.github.synnerz.devonian.api.dungeon.mapEnums.DoorTypes
import net.minecraft.block.Blocks

class DungeonDoor(var comps: MutableList<Double>) {
    var rotation: Int = -1
    var opened = false
    var type: DoorTypes = DoorTypes.NORMAL

    init {
        if (comps[0] != 0.0 && comps[1] != 0.0)
            checkType()
    }

    override fun toString(): String {
        return "DungeonDoor[type=\"$type\", rotation=\"$rotation\", opened=\"$opened\"]"
    }

    fun check() {
        val x = comps[0]
        val z = comps[1]
        if (!WorldUtils.isChunkLoaded(x, z)) return

        val blockId = WorldUtils.getBlockState(x, 69.0, z) ?: return

        opened = blockId.isAir
    }

    private fun checkType() {
        val x = comps[0]
        val z = comps[1]
        if (!WorldUtils.isChunkLoaded(x, z)) return

        val blockId = WorldUtils.getBlockState(x, 69.0, z) ?: return

        if (blockId.isAir || blockId == Blocks.BARRIER) return

        if (blockId == Blocks.INFESTED_COBBLESTONE) type = DoorTypes.ENTRANCE
        if (blockId == Blocks.COAL_BLOCK) type = DoorTypes.WITHER
        if (blockId == Blocks.RED_TERRACOTTA) type = DoorTypes.BLOOD

        opened = false
    }
}