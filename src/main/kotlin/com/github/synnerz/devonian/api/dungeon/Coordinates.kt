package com.github.synnerz.devonian.api.dungeon

val cornerStart = WorldPosition(-200, -200)
val cornerEnd = WorldPosition(-10, -10)
const val dungeonRoomSize = 31
const val dungeonDoorSize = 1
const val roomDoorCombinedSize = dungeonRoomSize + dungeonDoorSize
const val halfRoomSize = dungeonRoomSize / 2
const val halfCombinedSize = roomDoorCombinedSize / 2

data class WorldPosition(val x: Int, val z: Int) {
    override fun toString() = if (this == EMPTY) "World()" else "World($x, $z)"

    fun toComponent() =
        ComponentPosition(
            (x - cornerStart.x) / halfCombinedSize,
            (z - cornerStart.z) / halfCombinedSize,
        )

    fun withComponent() = toComponent().let { WorldComponentPosition(x, z, it.x, it.z) }

    companion object {
        val EMPTY = WorldPosition(Int.MIN_VALUE, Int.MIN_VALUE)
    }
}

data class ComponentPosition(val x: Int, val z: Int) {
    override fun toString() = if (this == EMPTY) "Component()" else "Component($x, $z)"

    fun toWorld() =
        WorldPosition(
            cornerStart.x + halfRoomSize + halfCombinedSize * x,
            cornerStart.z + halfRoomSize + halfCombinedSize * z,
        )

    fun withWorld() = toWorld().let { WorldComponentPosition(it.x, it.z, x, z) }

    fun isValid() = x in 0 .. 10 && z in 0 .. 10
    fun isValidRoom() = (x and 1) == 0 && (z and 1) == 0
    fun isValidDoor() = ((x and 1) xor (z and 1)) == 1

    fun getNeighboringRooms(): List<ComponentPosition> {
        if (isValidDoor()) return (
            if ((x and 1) == 1) listOf(
                ComponentPosition(x - 1, z),
                ComponentPosition(x + 1, z),
            )
            else listOf(
                ComponentPosition(x, z - 1),
                ComponentPosition(x, z + 1),
            )
        ).filter { it.isValid() }

        if (isValidRoom()) return listOf(
            ComponentPosition(x, z - 2),
            ComponentPosition(x, z + 2),
            ComponentPosition(x - 2, z),
            ComponentPosition(x + 2, z),
        ).filter { it.isValid() }

        return emptyList()
    }

    fun getNeighboringDoors(): List<ComponentPosition> {
        if (isValidRoom()) return listOf(
            ComponentPosition(x, z - 1),
            ComponentPosition(x, z + 1),
            ComponentPosition(x - 1, z),
            ComponentPosition(x + 1, z),
        ).filter { it.isValid() }

        return emptyList()
    }

    fun getNeighbors(): List<Neighbor> {
        if (!isValidRoom()) return emptyList()
        return listOf(
            Neighbor(ComponentPosition(x, z - 2), ComponentPosition(x, z - 1)),
            Neighbor(ComponentPosition(x, z + 2), ComponentPosition(x, z + 1)),
            Neighbor(ComponentPosition(x - 2, z), ComponentPosition(x - 1, z)),
            Neighbor(ComponentPosition(x + 2, z), ComponentPosition(x + 1, z)),
        ).filter { it.room.isValid() }
    }

    data class Neighbor(val room: ComponentPosition, val door: ComponentPosition)

    fun getRoomIdx() = (z / 2) * 6 + x / 2
    fun getDoorIdx() = ((x - 1) / 2 + 6 * z).let { it - it / 12 }

    companion object {
        val EMPTY = ComponentPosition(Int.MIN_VALUE, Int.MIN_VALUE)
    }
}

data class WorldComponentPosition(val wx: Int, val wz: Int, val cx: Int, val cz: Int) {
    override fun toString(): String = if (this == EMPTY) "WorldComp()" else "WorldComp($wx, $wz, $cx, $cz)"

    fun toWorld() = WorldPosition(wx, wz)
    fun toComponent() = ComponentPosition(cx, cz)

    companion object {
        val EMPTY = WorldComponentPosition(Int.MIN_VALUE, Int.MIN_VALUE, Int.MIN_VALUE, Int.MIN_VALUE)
    }
}

/*
 * r is radians cw of +x
 * _______\   +x
 * |      /
 * |
 * |
 * V  +z
 */
data class PlayerComponentPosition(val x: Double, val z: Double, val r: Double) {
    override fun toString(): String = "PlayerPosition(%.3f, %.3f, %.3f)".format(x, z, r)

    fun toComponent() = ComponentPosition(x.toInt(), z.toInt())
}