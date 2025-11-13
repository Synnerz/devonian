package com.github.synnerz.devonian.api.dungeon

import com.github.synnerz.devonian.api.WorldUtils
import com.github.synnerz.devonian.api.dungeon.mapEnums.CheckmarkTypes
import com.github.synnerz.devonian.api.dungeon.mapEnums.RoomTypes
import net.minecraft.block.Blocks

class DungeonRoom(var comps: MutableList<MutableList<Double>>, var height: Double) {
    val roomOffset = listOf(
        listOf(-DungeonScanner.halfRoomSize, -DungeonScanner.halfRoomSize),
        listOf(DungeonScanner.halfRoomSize, -DungeonScanner.halfRoomSize),
        listOf(DungeonScanner.halfRoomSize, DungeonScanner.halfRoomSize),
        listOf(-DungeonScanner.halfRoomSize, DungeonScanner.halfRoomSize)
    )
    var realComps = listOf<List<Double>>()
    var cores = listOf<Int>()
    var explored = false
    var name: String? = null
    var corner = listOf<Double>()
    var rotation = -1
    var type = RoomTypes.UNKNOWN
    var checkmark = CheckmarkTypes.UNEXPLORED
    var shape = "1x1"

    init {
        addComponents(comps)
    }

    override fun toString(): String {
        return "DungeonRoom[name=\"$name\"" +
                ", type=\"$type\"" +
                ", rotation=\"$rotation\"" +
                ", shape=\"$shape\"" +
                ", checkmark=\"$checkmark\"" +
                ", corner=\"$corner\"" +
                ", cores=\"$cores\"" +
                "]"
    }

    private fun loadFromData(data: DungeonScanner.RoomData) {
        cores = data.cores
        name = data.name
        type = RoomTypes.byName(data.type) ?: RoomTypes.NORMAL
    }

    private fun loadFromCore(core: Int): Boolean {
        for (room in DungeonScanner.roomsData) {
            if (!room.cores.contains(core)) continue

            loadFromData(room)
            return true
        }

        return false
    }

    internal fun update() {
        comps.sortWith(compareBy({ it[1] }, { it[0] }))
        realComps = comps.map { DungeonScanner.toPos(it[0], it[1]) }
        scan()

        shape()

        corner = listOf()
        rotation = -1
    }

    fun scan() = apply {
        checkmark = CheckmarkTypes.UNEXPLORED
        for (comp in realComps) {
            val ( x, z ) = comp
            if (!WorldUtils.isChunkLoaded(x, z)) continue
            if (height == 0.0)
                height = DungeonScanner.getHighestY(x, z)!!

            loadFromCore(DungeonScanner.hashCeil(x, z))
        }
    }

    internal fun hasComponent(x: Double, z: Double): Boolean {
        for (comp in comps) {
            val ( x1, z1 ) = comp
            if (x == x1 && z == z1) return true
        }

        return false
    }

    internal fun addComponent(x: Double, z: Double, update: Boolean = true) = apply {
        if (hasComponent(x, z)) return@apply

        comps.add(mutableListOf(x, z))

        if (update) update()
    }

    internal fun addComponent(comps: List<Double>, update: Boolean = true) = addComponent(comps[0], comps[1], update)

    internal fun addComponents(comps: List<List<Double>>) = apply {
        for (comp in comps) addComponent(comp, false)
        update()
    }

    internal fun findRotation() {
        if (height == 0.0) return

        if (type == RoomTypes.FAIRY) {
            val ( x, z ) = realComps[0]
            rotation = 0
            corner = listOf(
                x - DungeonScanner.halfRoomSize + 0.5,
                height,
                z - DungeonScanner.halfRoomSize + 0.5
            )
            return
        }

        for (comp in realComps) {
            val ( x, z ) = comp

            for (idx in roomOffset.indices) {
                val ( dx, dz ) = roomOffset[idx]
                val ( nx, nz ) = listOf(x + dx, z + dz)
                if (!WorldUtils.isChunkLoaded(nx, nz)) continue

                val blockState = WorldUtils.getBlockState(nx, height, nz) ?: continue
                val block = blockState.block ?: continue
                if (block != Blocks.BLUE_TERRACOTTA) continue

                rotation = idx * 90
                corner = listOf(nx + 0.5, height, nz + 0.5)
                break
            }
        }
    }

    private fun shape() {
        val distCompA = comps.map { it[0] }.distinct().size
        val distCompB = comps.map { it[1] }.distinct().size

        shape = when {
            comps.isEmpty() || comps.size > 4 -> "Unknown"
            comps.size == 1 -> "1x1"
            comps.size == 2 -> "1x2"
            comps.size == 4 -> if (distCompA == 1 || distCompB == 1) "1x4" else "2x2"
            distCompA == comps.size || distCompB == comps.size -> "1x3"
            else -> "L"
        }
    }
}