package com.github.synnerz.devonian.api.dungeon

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.WorldUtils
import com.github.synnerz.devonian.api.dungeon.mapEnums.DoorTypes
import com.github.synnerz.devonian.api.dungeon.mapEnums.RoomTypes
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.utils.Location
import com.google.gson.Gson
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.SlabBlock
import net.minecraft.registry.tag.FluidTags
import kotlin.math.floor

@Suppress("MemberVisibilityCanBePrivate")
object DungeonScanner {
    data class RoomData(
        val name: String,
        val type: String,
        val secrets: Int,
        val cores: List<Int>,
        val roomID: Int,
        val clear: String?,
        val crypts: Int,
        val clearScore: Int?,
        val secretScore: Int?,
        val shape: String
    ) {
        override fun toString(): String {
            return "RoomData[name=\"$name\", type=\"$type\", secrets=\"$secrets\"," +
                    " cores=\"$cores\", roomId=\"$roomID\"" +
                    ", clear=\"$clear\", crypts=\"$crypts\", clearScore=\"$clearScore\"," +
                    " secretScore=\"$secretScore\", shape=\"$shape\"]"
        }
    }

    val roomsData = Gson().fromJson(
        this::class.java.getResourceAsStream("/assets/devonian/dungeons/rooms.json")
            ?.bufferedReader()
            .use { it?.readText() },
        Array<RoomData>::class.java
    ).toList()
    private val cornerStart = listOf(-200, -200)
    private val cornerEnd = listOf(-10, -10)
    private const val dungeonRoomSize = 31
    private const val dungeonDoorSize = 1
    private const val roomDoorCombinedSize = dungeonRoomSize + dungeonDoorSize
    internal val halfRoomSize = floor(dungeonRoomSize / 2.0)
    internal val halfCombinedSize = floor(roomDoorCombinedSize / 2.0)
    private val directions: List<List<Double>> = listOf(
        listOf(halfCombinedSize, 0.0, 1.0, 0.0),
        listOf(-halfCombinedSize, 0.0, -1.0, 0.0),
        listOf(0.0, halfCombinedSize, 0.0, 1.0),
        listOf(0.0, -halfCombinedSize, 0.0, -1.0)
    )
    val defaultMapSize = listOf(125, 125)

    var lastIdx: Int? = null
    var currentRoom: DungeonRoom? = null
    var rooms = MutableList<DungeonRoom?>(36) { null }
    var doors = MutableList<DungeonDoor?>(60) { null }
    var availablePos = findAvailablePos()

    @JvmOverloads
    internal fun toPos(x: Double, z: Double, doors: Boolean = false): List<Double> {
        val ( x0, z0 ) = cornerStart
        if (doors) return listOf(
            x0 + halfRoomSize + halfCombinedSize * x,
            z0 + halfRoomSize + halfCombinedSize * z
        )

        return listOf(
            x0 + halfRoomSize + roomDoorCombinedSize * x,
            z0 + halfRoomSize + roomDoorCombinedSize * z
        )
    }

    @JvmOverloads
    internal fun toComponent(x: Double, z: Double, doors: Boolean = false): List<Double> {
        val ( x0, z0 ) = cornerStart
        val size = if (doors) halfCombinedSize else roomDoorCombinedSize.toDouble()

        return listOf(
            floor((x - x0 + 0.5) / size),
            floor((z - z0 + 0.5) / size),
        )
    }

    private fun findAvailablePos(): MutableList<List<Double>> {
        val pos = mutableListOf<List<Double>>()

        for (z in 0..10) {
            for (x in 0..10) {
                if (x % 2 != 0 && z % 2 != 0) continue

                val rx = cornerStart[0] + halfRoomSize + x * halfCombinedSize
                val rz = cornerStart[0] + halfRoomSize + z * halfCombinedSize

                pos.add(listOf(x.toDouble(), z.toDouble(), rx, rz))
            }
        }

        return pos
    }

    internal fun getHighestY(x: Double, z: Double): Double? {
        WorldUtils.world ?: return null
        var height = 0.0

        for (idx in 256 downTo 0) {
            val blockState = WorldUtils.getBlockState(x, idx.toDouble(), z)
            val block = blockState?.block ?: continue

            if (blockState.isAir || block == Blocks.GOLD_BLOCK) continue

            height = idx.toDouble()
            break
        }

        return height
    }

    private fun getLegacyId(blockState: BlockState, debug: Boolean = false): Int? {
        val block = blockState.block
        var registryName = WorldUtils.registryName(block)
        val fluidState = blockState.fluidState
        if (!fluidState.isEmpty) {
            if (fluidState.isIn(FluidTags.WATER))
                return if (fluidState.isStill) 9 else 8
            if (fluidState.isIn(FluidTags.LAVA))
                return if (fluidState.isStill) 11 else 10
        }

        if (block is SlabBlock)
            registryName += "[type=${blockState.get(SlabBlock.TYPE).name.lowercase()}]"
        val result = LegacyRegistry.BLOCKS[registryName]

        // TODO: either remove or make it part of debug tools
        if (result == null) println("Devonian\$DungeonScanner[state=\"Could not find registry\", name=\"$registryName\"]")
        if (debug) println("Devonian\$DebugScanner[registry=\"$registryName\"]")

        return result
    }

    @JvmOverloads
    internal fun hashCeil(x: Double, z: Double, debug: Boolean = false): Int {
        var str = ""

        for (idx in 140 downTo 12) {
            val blockState = WorldUtils.getBlockState(x, idx.toDouble(), z) ?: continue
            val block = blockState.block ?: continue
            val blockId = getLegacyId(blockState, debug) ?: continue
            if (block == Blocks.IRON_BARS || block == Blocks.CHEST) {
                str += "0"
                continue
            }
            str += blockId
        }

        return str.hashCode()
    }

    private fun checkDoorState() {
        for (door in doors) {
            if (door == null || door.opened) continue

            door.check()
        }
    }

    private fun checkRoomState() {
        for (room in rooms) {
            if (room == null) continue
            if (room.rotation != -1) continue

            room.findRotation()
        }
    }

    init {
        rooms.fill(null)
        doors.fill(null)

        val tickEvent = EventBus.on<TickEvent> ({
            if (Location.area != "catacombs") return@on
            val player = Devonian.minecraft.player ?: return@on
            if (!WorldUtils.isChunkLoaded(player.x, player.z)) return@on
            val ( x, z ) = toComponent(player.x, player.z)
            val jdx = (6 * z + x).toInt()

            scan()
            checkRoomState()
            checkDoorState()
            DungeonMapScanner.checkPlayerState()

            if (jdx > 35) return@on

            if (lastIdx == jdx) return@on
            lastIdx = jdx
            currentRoom = rooms[jdx]
            // TODO: remove whenever done debugging
            ChatUtils.sendMessage("$currentRoom")
        }, false)

        EventBus.on<AreaEvent> { event ->
            val area = event.area
            if (area == null || area != "Catacombs")
                return@on reset()

            tickEvent.add()
        }
    }

    fun init() {
        // TODO: remove this whenever finished (or impl in debug tools)

        DevonianCommand.command.subcommand("getcore") { _, _ ->
            val room = getRoomAt(Devonian.minecraft.player!!.x, Devonian.minecraft.player!!.z) ?: return@subcommand 0
            for (comp in room.realComps) {
                val ( x, z ) = comp
                ChatUtils.sendMessage("hash: ${hashCeil(x, z, true)}")
            }
            1
        }

        DevonianCommand.command.subcommand("area") { _, args ->
            val str = (args.firstOrNull() ?: return@subcommand 0) as String
            AreaEvent(str).post()
            Location.area = str.lowercase()
            ChatUtils.sendMessage("&aPosting area event with str &6$str", true)
            1
        }.string("name")

        DevonianCommand.command.subcommand("subarea") { _, args ->
            val str = (args.firstOrNull() ?: return@subcommand 0) as String
            SubAreaEvent(str).post()
            Location.subarea = str
            ChatUtils.sendMessage("&aPosting subarea event with str &6$str", true)
            1
        }.string("name")
    }

    fun getRoomIdx(cx: Int, cz: Int): Int {
        return 6 * cz + cx
    }

    fun getRoomIdx(cx: Double, cz: Double) = getRoomIdx(cx.toInt(), cz.toInt())

    fun mergeRooms(room1: DungeonRoom, room2: DungeonRoom) {
        for (comp in room2.comps) {
            if (!room1.hasComponent(comp[0], comp[1]))
                room1.addComponent(comp, false)

            addRoom(getRoomIdx(comp[0], comp[1]), room1)
        }

        room1.update()
    }

    fun getDoorIdx(x: Int, z: Int): Int {
        if (x !in 0..10 || z !in 0..10) return -1
        val idx = ((x - 1) shr 1) + 6 * z
        return idx - (idx / 12)
    }

    fun getDoorIdx(x: Double, z: Double) = getDoorIdx(x.toInt(), z.toInt())

    fun addDoor(door: DungeonDoor) {
        val cx = door.comps[2].toInt()
        val cz = door.comps[3].toInt()
        val idx = getDoorIdx(cx, cz)
        if (idx !in 0 .. 59) return

        doors[idx] = door
        if (cx and 1 == 0) {
            rooms.getOrNull(getRoomIdx(cx shr 1, (cz - 1) shr 1))?.also {
                it.doors.add(door)
                door.rooms.add(it)
            }
            rooms.getOrNull(getRoomIdx(cx shr 1, (cz + 1) shr 1))?.also {
                it.doors.add(door)
                door.rooms.add(it)
            }
        } else {
            rooms.getOrNull(getRoomIdx((cx - 1) shr 1, cz shr 1))?.also {
                it.doors.add(door)
                door.rooms.add(it)
            }
            rooms.getOrNull(getRoomIdx((cx + 1) shr 1, cz shr 1))?.also {
                it.doors.add(door)
                door.rooms.add(it)
            }
        }
    }

    fun addRoom(idx: Int, room: DungeonRoom) {
        rooms[idx] = room

        val cx = (idx % 6) shl 1
        val cz = (idx / 6) shl 1
        doors.getOrNull(getDoorIdx(cx + 0, cz - 1))?.also {
            it.rooms.add(room)
            room.doors.add(it)
        }
        doors.getOrNull(getDoorIdx(cx + 0, cz + 1))?.also {
            it.rooms.add(room)
            room.doors.add(it)
        }
        doors.getOrNull(getDoorIdx(cx - 1, cz + 0))?.also {
            it.rooms.add(room)
            room.doors.add(it)
        }
        doors.getOrNull(getDoorIdx(cx + 1, cz + 0))?.also {
            it.rooms.add(room)
            room.doors.add(it)
        }
    }

    fun getRoomAt(x: Double, z: Double): DungeonRoom? {
        val ( dx, dz ) = toComponent(x, z)
        val idx = getRoomIdx(dx, dz)
        if (idx !in 0 .. 35) return null

        return rooms[idx]
    }

    internal fun reset() {
        rooms.fill(null)
        doors.fill(null)
        lastIdx = null
        currentRoom = null
        availablePos = findAvailablePos()
    }

    internal fun scan() {
        if (availablePos.isEmpty()) return

        availablePos.reversed().forEachIndexed { idx, pos ->
            var ( x, z, rx, rz ) = pos
            if (!WorldUtils.isChunkLoaded(rx, rz)) return@forEachIndexed

            availablePos.remove(pos)

            val roofHeight = getHighestY(rx, rz) ?: return@forEachIndexed

            // Door scan
            if (x % 2 == 1.0 || z % 2 == 1.0) {
                if (roofHeight < 85) {
                    val door = DungeonDoor(mutableListOf(rx, rz, x, z))
                    if (z % 2 == 1.0) door.rotation = 0

                    addDoor(door)
                }
                return@forEachIndexed
            }

            x /= 2.0
            z /= 2.0

            val cdx = getRoomIdx(x, z)
            val room = DungeonRoom(mutableListOf(mutableListOf(x, z)), roofHeight).scan()
            addRoom(cdx, room)

            for (dir in directions) {
                val ( dx0, dz0, dx1, dz1 ) = dir
                val nx0 = rx + dx0
                val nz0 = rz + dz0

                val heightBlock = WorldUtils.getBlockState(nx0, roofHeight, nz0) ?: continue
                val aboveHeightBlock = WorldUtils.getBlockState(nx0, roofHeight + 1.0, nz0) ?: continue
                val heightBlockId = WorldUtils.getBlockId(heightBlock.block)
                val aboveHeightId = WorldUtils.getBlockId(aboveHeightBlock.block)

                if (room.type == RoomTypes.ENTRANCE && heightBlockId != 0) {
                    val block1Id = WorldUtils.getBlockState(nx0, 76.0, nz0) ?: continue
                    if (block1Id.isAir) continue
                    val ( doorx, doorz ) = listOf(x * 2 + dx1, z * 2 + dz1)
                    val doorIdx = getDoorIdx(doorx, doorz)
                    if (doorIdx in 0..60) {
                        val door = DungeonDoor(mutableListOf(nx0, nz0, doorx, doorz))
                        door.type = DoorTypes.ENTRANCE
                        addDoor(door)
                    }
                    continue
                }

                if (heightBlockId == 0 || aboveHeightId != 0) continue

                val ( newX, newZ ) = listOf(x + dx1, z + dz1)
                val ndx = getRoomIdx(newX, newZ)
                if (ndx !in 0 .. 35) continue

                val nroom = rooms[ndx]
                if (nroom == null) {
                    room.addComponent(newX, newZ)
                    addRoom(ndx, room)
                    continue
                }

                if (nroom.type == RoomTypes.ENTRANCE || nroom == room) continue

                mergeRooms(nroom, room)
            }
        }
    }
}