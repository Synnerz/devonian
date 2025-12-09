package com.github.synnerz.devonian.api.dungeon

import com.github.synnerz.devonian.api.WorldUtils
import com.github.synnerz.devonian.api.dungeon.mapEnums.CheckmarkTypes
import com.github.synnerz.devonian.api.dungeon.mapEnums.ClearTypes
import com.github.synnerz.devonian.api.dungeon.mapEnums.RoomTypes
import com.github.synnerz.devonian.api.dungeon.mapEnums.ShapeTypes
import net.minecraft.world.level.block.Blocks
import kotlin.math.floor

class DungeonRoom(comps: List<WorldComponentPosition>, var height: Int) {
    val comps = mutableListOf<WorldComponentPosition>()
    private val possibleCorners = mutableListOf<Triple<Int, WorldComponentPosition, WorldPosition>>()
    var cores = listOf<Int>()
    var explored = false
    var name: String? = null
    var roomID: Int? = null
    var corner = WorldPosition.EMPTY
    var rotation = -1
    var type = RoomTypes.UNKNOWN
    var checkmark = CheckmarkTypes.UNEXPLORED
    var shape = ShapeTypes.Shape1x1
    var totalSecrets = 0
    var secretsCompleted = -1
    var clear = ClearTypes.MOB
    val doors = mutableSetOf<DungeonDoor>()
    private var shapeIn = ""

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
        roomID = data.roomID
        type = RoomTypes.byName(data.type) ?: RoomTypes.NORMAL
        clear = when (data.clear) {
            "mob" -> ClearTypes.MOB
            "miniboss" -> ClearTypes.MINIBOSS
            else -> ClearTypes.OTHER
        }
        totalSecrets = data.secrets
        shapeIn = data.shape
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
        comps.sortWith(compareBy<WorldComponentPosition> { it.cx }.thenBy { it.cz })

        scan()
        shape()
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

        val w = comp.withWorld()
        comps.add(w)
        roomOffset.forEachIndexed { i, v ->
            possibleCorners.add(
                Triple(
                    i,
                    w,
                    WorldPosition(
                        w.wx + v.x,
                        w.wz + v.z
                    )
                )
            )
        }

        if (update) update()
    }

    fun addComponents(comps: List<ComponentPosition>) = apply {
        for (comp in comps) addComponent(comp, false)
        update()
    }

    fun findRotation() {
        if (height == 0) return
        if (shapeIn == "1x4" && comps.size < 4) return

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

        possibleCorners.removeIf { (idx, comp, pos) ->
            if (shapeIn == "1x4") {
                val cidx = comps.indexOf(comp)
                if (cidx != 0 && cidx != comps.size - 1) return@removeIf true
                val isHorz = comps[0].cz == comps[1].cz
                if (cidx == 0) {
                    if (isHorz) {
                        if (idx != 0 && idx != 3) return@removeIf true
                    } else {
                        if (idx != 0 && idx != 1) return@removeIf true
                    }
                } else {
                    if (isHorz) {
                        if (idx != 1 && idx != 2) return@removeIf true
                    } else {
                        if (idx != 2 && idx != 3) return@removeIf true
                    }
                }
            }
            val x = pos.x
            val z = pos.z
            if (!WorldUtils.isChunkLoaded(x, z)) return@removeIf false

            val blockState = WorldUtils.getBlockState(x, height, z) ?: return@removeIf false
            val block = blockState.block ?: return@removeIf false
            if (block != Blocks.BLUE_TERRACOTTA) return@removeIf true

            rotation = idx * 90
            corner = pos
            true
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

    private fun rotatePos(x: Int, z: Int, degree: Int): Pair<Int, Int> {
        return when (degree) {
            0 -> x to z
            90 -> z to -x
            180 -> -x to -z
            270 -> -z to x
            else -> x to z
        }
    }

    /**
     * - Converts real world position coordinates into relative component position
     * - NOTE: the Y value is always the same regardless, that never changes
     * @param x
     * @param z
     */
    fun fromPos(x: Int, z: Int): Pair<Int, Int>? {
        if (rotation == -1 || corner == WorldPosition.EMPTY) return null
        val x1 = x - floor(corner.x + 0.5).toInt()
        val z1 = z - floor(corner.z + 0.5).toInt()

        return rotatePos(x1, z1, rotation)
    }

    /**
     * - Converts component positions into real world position coordinates
     * - NOTE: the Y value is always the same regardless, that never changes
     * @param x
     * @param z
     */
    fun fromComp(x: Int, z: Int): Pair<Int, Int>? {
        if (rotation == -1 || corner == WorldPosition.EMPTY) return null
        val ( x1, z1 ) = rotatePos(x, z, 360 - rotation)
        val x2 = x1 + corner.x
        val z2 = z1 + corner.z

        return x2 to z2
    }

    companion object {
        val roomOffset = listOf(
            WorldPosition(-halfRoomSize, -halfRoomSize),
            WorldPosition(halfRoomSize, -halfRoomSize),
            WorldPosition(halfRoomSize, halfRoomSize),
            WorldPosition(-halfRoomSize, halfRoomSize)
        )
    }
}