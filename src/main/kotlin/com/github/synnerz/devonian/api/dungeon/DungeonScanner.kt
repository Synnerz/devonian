package com.github.synnerz.devonian.api.dungeon

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.WorldUtils
import com.github.synnerz.devonian.api.dungeon.mapEnums.CheckmarkTypes
import com.github.synnerz.devonian.api.dungeon.mapEnums.DoorTypes
import com.github.synnerz.devonian.api.dungeon.mapEnums.RoomTypes
import com.github.synnerz.devonian.api.events.AreaEvent
import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.api.events.SubAreaEvent
import com.github.synnerz.devonian.api.events.TickEvent
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.features.dungeons.map.DungeonMap
import com.github.synnerz.devonian.utils.Location
import com.google.gson.Gson
import net.minecraft.tags.FluidTags
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.SlabBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.HitResult

@Suppress("MemberVisibilityCanBePrivate")
object DungeonScanner {
    data class RoomData(
        val name: String,
        val type: String,
        val secrets: Int,
        val cores: List<Int>,
        val trappedChests: Int,
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

    var lastIdx: Int? = null
    var currentRoom: DungeonRoom? = null
    var rooms = MutableList<DungeonRoom?>(36) { null }
    var doors = MutableList<DungeonDoor?>(60) { null }
    var availablePos = findAvailablePos()

    private fun findAvailablePos(): MutableList<WorldComponentPosition> {
        val pos = mutableListOf<WorldComponentPosition>()

        for (z in 0 .. 10) {
            for (x in 0 .. 10) {
                if (x % 2 != 0 && z % 2 != 0) continue

                pos.add(ComponentPosition(x, z).withWorld())
            }
        }

        return pos
    }

    fun getHighestY(x: Int, z: Int): Int {
        WorldUtils.world ?: return -1
        var height = 0

        for (idx in 256 downTo 0) {
            val blockState = WorldUtils.getBlockState(x, idx, z)
            val block = blockState?.block ?: continue

            if (blockState.isAir || block == Blocks.GOLD_BLOCK) continue

            height = idx
            break
        }

        return height
    }

    private fun getLegacyId(blockState: BlockState, debug: Boolean = false): Int? {
        val block = blockState.block
        var registryName = WorldUtils.registryName(block)
        val fluidState = blockState.fluidState
        if (!fluidState.isEmpty) {
            if (fluidState.`is`(FluidTags.WATER))
                return if (fluidState.isSource) 9 else 8
            if (fluidState.`is`(FluidTags.LAVA))
                return if (fluidState.isSource) 11 else 10
        }

        if (block is SlabBlock)
            registryName += "[type=${blockState.getValue(SlabBlock.TYPE).name.lowercase()}]"
        val result = LegacyRegistry.BLOCKS[registryName]

        // TODO: either remove or make it part of debug tools
        if (result == null) println("Devonian\$DungeonScanner[state=\"Could not find registry\", name=\"$registryName\"]")
        if (debug) println("Devonian\$DebugScanner[registry=\"$registryName\"]")

        return result
    }

    @JvmOverloads
    fun hashCeil(x: Int, z: Int, debug: Boolean = false): Int {
        var str = ""

        for (idx in 140 downTo 12) {
            val blockState = WorldUtils.getBlockState(x, idx, z) ?: continue
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

        EventBus.on<TickEvent> {
            if (Location.area != "catacombs") return@on
            if (Dungeons.inBoss.value) return@on
            val player = Devonian.minecraft.player ?: return@on
            if (!WorldUtils.isChunkLoaded(player.x, player.z)) return@on
            val comp = WorldPosition(player.x.toInt(), player.z.toInt()).toComponent()
            val jdx = comp.getRoomIdx()

            scan()
            checkRoomState()
            checkDoorState()

            // Will trigger for each component of the room
            // perhaps hold the current room's name and match
            // it with that, so we can be sure it's not the same room
            if (lastIdx != null && lastIdx != jdx)
                DungeonEvent.RoomLeave(currentRoom, lastIdx!!).post()

            if (jdx !in 0..35) return@on

            if (lastIdx == jdx) return@on
            lastIdx = jdx
            currentRoom = rooms.getOrNull(jdx)
            if (currentRoom != null)
                DungeonEvent.RoomEnter(currentRoom!!, jdx)
            // TODO: remove whenever done debugging
            ChatUtils.sendMessage("$currentRoom")
        }
    }

    fun init() {
        // TODO: remove this whenever finished (or impl in debug tools)

        DevonianCommand.command.subcommand("getcore") { _, _ ->
            val player = Devonian.minecraft.player ?: return@subcommand 0
            val comp = WorldPosition(player.x.toInt(), player.z.toInt()).toComponent()
            val room = rooms.getOrNull(comp.getRoomIdx()) ?: return@subcommand 0
            for (comp in room.comps) {
                val x = comp.wx
                val z = comp.wz
                ChatUtils.sendMessage("hash: ${hashCeil(x, z, true)}")
            }
            1
        }

        DevonianCommand.command.subcommand("getpuzzledata") { _, _ ->
            rooms.forEach {
                if (it == null || it.type != RoomTypes.PUZZLE) return@forEach
                ChatUtils.sendMessage("&dPuzzle[name=&b\"${it.name}\"&d, rotation=&b\"${it.rotation}\"&d]")
            }
            1
        }

        DevonianCommand.command.subcommand("getcomp") { _, _ ->
            Devonian.minecraft.player ?: return@subcommand 0
            val target = Devonian.minecraft.hitResult ?: return@subcommand 0
            if (target.type != HitResult.Type.BLOCK) return@subcommand 0
            val x = target.location.x.toInt()
            val z = target.location.z.toInt()
            val comp = WorldPosition(x, z).toComponent()
            val room = rooms.getOrNull(comp.getRoomIdx()) ?: return@subcommand 0
            val relativeCoords = room.fromPos(x, z) ?: return@subcommand 0

            ChatUtils.sendMessage("looking at component \"${relativeCoords.first}, ${target.location.y.toInt()}, ${relativeCoords.second}\" ${room.name} with rotation ${room.rotation}")
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

    fun mergeRooms(comp1: ComponentPosition, comp2: ComponentPosition): Boolean {
        val i1 = comp1.getRoomIdx()
        val i2 = comp2.getRoomIdx()
        val r1 = rooms[comp1.getRoomIdx()]
        val r2 = rooms[comp2.getRoomIdx()]

        if (r1 != null && r2 != null) {
            if (i1 < i2) mergeRooms(r1, r2)
            else mergeRooms(r2, r1)
            return true
        }
        if (r1 == null && r2 == null) return false
        val r: DungeonRoom
        val c: ComponentPosition
        if (r1 == null) {
            r = r2!!
            c = comp1
        } else {
            r = r1
            c = comp2
        }

        r.addComponent(c)
        addRoom(c, r)
        return true
    }

    fun mergeRooms(room1: DungeonRoom, room2: DungeonRoom) {
        if (room1 === room2) return
        for (comp in room2.comps) {
            val c = comp.toComponent()
            room1.addComponent(c, false)

            addRoom(c, room1, true)
        }

        room1.update()

        room2.doors.forEach { it.rooms.remove(room2) }
    }

    fun addDoor(door: DungeonDoor) {
        val comp = door.comp.toComponent()
        val idx = comp.getDoorIdx()
        if (idx !in 0 .. 59) return

        doors[idx] = door
        comp.getNeighboringRooms().forEach {
            rooms.getOrNull(it.getRoomIdx())?.also {
                it.doors.add(door)
                door.rooms.add(it)
            }
        }
    }

    fun addRoom(comp: ComponentPosition, room: DungeonRoom, force: Boolean = false) {
        val idx = comp.getRoomIdx()
        if (idx !in 0 .. 35) return
        if (!force) {
            rooms[idx]?.also {
                if (room.name == null) mergeRooms(it, room)
                else mergeRooms(room, it)
                return
            }
        }
        rooms[idx] = room

        comp.getNeighboringDoors().forEach {
            doors.getOrNull(it.getDoorIdx())?.also {
                it.rooms.add(room)
                room.doors.add(it)
            }
        }
    }

    fun reset() {
        rooms.fill(null)
        doors.fill(null)
        lastIdx = null
        currentRoom = null
        availablePos = findAvailablePos()
    }

    fun scan() {
        if (availablePos.isEmpty()) return

        val startLen = availablePos.size
        availablePos.reversed().forEachIndexed { idx, pos ->
            val (wx, wz, cx, cz) = pos
            val comp = pos.toComponent()
            if (!WorldUtils.isChunkLoaded(wx, wz)) return@forEachIndexed

            availablePos.remove(pos)

            val roofHeight = getHighestY(wx, wz)
            if (roofHeight < 0) return@forEachIndexed

            // Door scan
            if (comp.isValidDoor()) {
                if (roofHeight != 0 && roofHeight < 85) {
                    val door = DungeonDoor(pos)
                    if (cz % 2 == 1) door.rotation = 0

                    addDoor(door)
                }
                return@forEachIndexed
            }
            if (roofHeight <= 0) return@forEachIndexed

            var room = DungeonRoom(mutableListOf(pos), roofHeight).scan()
            if (room.type == RoomTypes.ENTRANCE) {
                room.explored = true
                room.checkmark = CheckmarkTypes.NONE
            }
            addRoom(comp, room)

            comp.getNeighbors().forEach { (posRoom, posDoor) ->
                val worldPos = posDoor.withWorld()
                val nx0 = worldPos.wx
                val nz0 = worldPos.wz

                val heightBlock = WorldUtils.getBlockState(nx0, roofHeight, nz0) ?: return@forEach
                val aboveHeightBlock = WorldUtils.getBlockState(nx0, roofHeight + 1, nz0) ?: return@forEach
                val heightBlockId = WorldUtils.getBlockId(heightBlock.block)
                val aboveHeightId = WorldUtils.getBlockId(aboveHeightBlock.block)

                if (room.type == RoomTypes.ENTRANCE && heightBlockId != 0) {
                    val block1Id = WorldUtils.getBlockState(nx0, 76, nz0) ?: return@forEach
                    if (block1Id.isAir) return@forEach
                    val doorIdx = posDoor.getDoorIdx()
                    if (doorIdx in 0 .. 60) {
                        val door = DungeonDoor(worldPos)
                        door.type = DoorTypes.ENTRANCE
                        addDoor(door)
                    }
                    return@forEach
                }

                if (heightBlockId == 0 || aboveHeightId != 0) return@forEach

                val ndx = posRoom.getRoomIdx()
                if (ndx !in 0 .. 35) return@forEach

                val nroom = rooms[ndx]
                if (nroom == null) {
                    room.addComponent(posRoom)
                    addRoom(posRoom, room)
                    return@forEach
                }

                if (nroom.type == RoomTypes.ENTRANCE || nroom == room) return@forEach

                mergeRooms(nroom, room)
                room = nroom
            }
        }

        if (availablePos.size != startLen) DungeonMap.redrawMap(rooms.toList(), doors.toList())
    }
}