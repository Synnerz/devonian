package com.github.synnerz.devonian.api.dungeon

import com.github.synnerz.devonian.api.WorldUtils
import com.github.synnerz.devonian.api.dungeon.mapEnums.CheckmarkTypes
import com.github.synnerz.devonian.api.dungeon.mapEnums.ClearTypes
import com.github.synnerz.devonian.api.dungeon.mapEnums.RoomTypes
import com.github.synnerz.devonian.api.dungeon.mapEnums.ShapeTypes
import net.minecraft.block.Blocks

class DungeonRoom(val comps: MutableList<WorldComponentPosition>, var height: Int) {
    val roomOffset = listOf(
        WorldPosition(-halfRoomSize, -halfRoomSize),
        WorldPosition(halfRoomSize, -halfRoomSize),
        WorldPosition(halfRoomSize, halfRoomSize),
        WorldPosition(-halfRoomSize, halfRoomSize)
    )
    var cores = listOf<Int>()
    var explored = false
    var name: String? = null
    var corner = WorldPosition.EMPTY
    var rotation = -1
    var type = RoomTypes.UNKNOWN
    var checkmark = CheckmarkTypes.UNEXPLORED
    var shape = ShapeTypes.Shape1x1
    var totalSecrets = 0
    var secretsCompleted = 0
    var clear = ClearTypes.MOB
    val doors = mutableSetOf<DungeonDoor>()

    init {
        addComponents(comps.map { it.toComponent() })
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
        clear = when (data.clear) {
            "mob" -> ClearTypes.MOB
            "miniboss" -> ClearTypes.MINIBOSS
            else -> ClearTypes.OTHER
        }
        totalSecrets = data.secrets
    }

    private fun loadFromCore(core: Int): Boolean {
        for (room in DungeonScanner.roomsData) {
            if (!room.cores.contains(core)) continue

            loadFromData(room)
            return true
        }

        return false
    }

    fun update() {
        comps.sortBy { it.cz * 11 + it.cx }

        scan()
        shape()

        corner = WorldPosition.EMPTY
        rotation = -1
    }

    fun scan() = apply {
        checkmark = CheckmarkTypes.UNEXPLORED
        for (comp in comps) {
            val x = comp.wx
            val z = comp.wz
            if (!WorldUtils.isChunkLoaded(x, z)) continue
            if (height == 0) height = DungeonScanner.getHighestY(x, z)

            loadFromCore(DungeonScanner.hashCeil(x, z))
        }
    }

    fun addComponent(comp: ComponentPosition, update: Boolean = true) = apply {
        if (comps.any { it.toComponent() == comp }) return@apply

        comps.add(comp.withWorld())

        if (update) update()
    }

    fun addComponents(comps: List<ComponentPosition>) = apply {
        for (comp in comps) addComponent(comp, false)
        update()
    }

    fun findRotation() {
        if (height == 0) return

        if (type == RoomTypes.FAIRY) {
            val x = comps[0].wx
            val z = comps[0].wz
            rotation = 0
            corner = WorldPosition(
                x - halfRoomSize,
                z - halfRoomSize
            )
            return
        }

        for (comp in comps) {
            val x = comps[0].wx
            val z = comps[0].wz

            for (idx in roomOffset.indices) {
                val ( dx, dz ) = roomOffset[idx]
                val pos = WorldPosition(x + dx, z + dz)
                val nx = pos.x
                val nz = pos.z
                if (!WorldUtils.isChunkLoaded(nx, nz)) continue

                val blockState = WorldUtils.getBlockState(nx, height, nz) ?: continue
                val block = blockState.block ?: continue
                if (block != Blocks.BLUE_TERRACOTTA) continue

                rotation = idx * 90
                corner = pos
                break
            }
        }
    }

    private fun shape() {
        val distCompA = comps.map { it.cx }.distinct().size
        val distCompB = comps.map { it.cz }.distinct().size

        shape = when {
            comps.isEmpty() || comps.size > 4 -> ShapeTypes.Unknown
            comps.size == 1 -> ShapeTypes.Shape1x1
            comps.size == 2 -> ShapeTypes.Shape1x2
            comps.size == 4 -> if (distCompA == 1 || distCompB == 1) ShapeTypes.Shape1x4 else ShapeTypes.Shape2x2
            distCompA == comps.size || distCompB == comps.size -> ShapeTypes.Shape1x3
            else -> ShapeTypes.ShapeL
        }
    }
}