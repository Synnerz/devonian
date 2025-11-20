package com.github.synnerz.devonian.api.dungeon

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.dungeon.mapEnums.CheckmarkTypes
import com.github.synnerz.devonian.api.dungeon.mapEnums.DoorTypes
import com.github.synnerz.devonian.api.dungeon.mapEnums.RoomTypes
import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.api.events.PacketReceivedEvent
import com.github.synnerz.devonian.features.dungeons.map.DungeonMap
import com.github.synnerz.devonian.utils.Location
import com.github.synnerz.devonian.utils.math.MathUtils
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket
import net.minecraft.world.item.MapItem
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes
import net.minecraft.world.level.saveddata.maps.MapItemSavedData
import kotlin.math.PI

object DungeonMapScanner {
    private const val COLOR_SIZE = 16384
    private const val SCAN = 128
    private const val ROOM_SPACING = 4
    var roomSize = -1
    var roomGap = -1
    var roomCount = -1
    var mapOffsetX = -1
    var mapOffsetZ = -1
    var mapWidth = -1
    var mapHeight = -1
    private val unscannedDoors = mutableSetOf<ComponentPosition>()

    fun reset() {
        roomSize = -1
        roomGap = -1
        roomCount = -1
        mapOffsetX - 1
        mapOffsetZ - 1
        mapWidth = -1
        mapHeight = -1
        unscannedDoors.clear()
        for (x in 0 .. 10) {
            for (z in (x and 1 xor 1) .. 10 step 2) {
                unscannedDoors.add(ComponentPosition(x, z))
            }
        }
    }

    private enum class MapColors(val color: Byte) {
        EMPTY(0),

        CHECK_WHITE(34),
        CHECK_GREEN(30),
        CHECK_FAIL(18),
        CHECK_UNKNOWN(119),

        ROOM_ENTRANCE(30),
        ROOM_NORMAL(63),
        ROOM_UNOPENED(85),
        ROOM_TRAP(62),
        ROOM_BOSS(74),
        ROOM_PUZZLE(66),
        ROOM_FAIRY(82),
        ROOM_BLOOD(18),

        DOOR_WITHER(119),
        DOOR_BLOOD(18);
    }

    private fun scanMapDimensions(colors: ByteArray): Boolean {
        val floor = Dungeons.floor
        if (floor == FloorType.None) return false

        var entranceIdx = 0
        var i = 0
        while (entranceIdx < colors.size && colors[entranceIdx] != MapColors.ROOM_ENTRANCE.color) {
            i++
            entranceIdx = ((i and 7) shl 4) + ((i shr 3) shl 11)
        }

        if (entranceIdx >= colors.size) return false

        var l = entranceIdx
        var r = entranceIdx
        while (colors[l - 1] == MapColors.ROOM_ENTRANCE.color) l--
        while (colors[r + 1] == MapColors.ROOM_ENTRANCE.color) r++

        var t = entranceIdx
        var b = entranceIdx
        while (colors[t - SCAN] == MapColors.ROOM_ENTRANCE.color) t -= SCAN
        while (colors[b + SCAN] == MapColors.ROOM_ENTRANCE.color) b += SCAN

        l = l and 127
        r = r and 127
        t = t shr 7
        b = b shr 7
        if (r - l + 1 != b - t + 1) println("map scanner: found non-square room?")
        roomSize = r - l + 1
        roomGap = roomSize + ROOM_SPACING

        mapOffsetX = l % roomGap
        mapOffsetZ = t % roomGap

        mapWidth = roomGap * (floor.roomsW - 1) + roomSize
        mapHeight = roomGap * (floor.roomsH - 1) + roomSize

        if (SCAN - mapWidth >= roomGap * 2) mapOffsetX += roomGap
        if (SCAN - mapHeight >= roomGap * 2) mapOffsetZ += roomGap

        return true
    }

    private fun updatePlayerIcons(mapState: MapItemSavedData) {
        if (Dungeons.players.isEmpty()) return

        val decorations = mapState.decorations.toList()
        if (Dungeons.players.filter { !it.value.isDead }.size != decorations.size) return

        val floor = Dungeons.floor
        if (floor == FloorType.None) return

        val playerIter = Dungeons.players.iterator()
        playerIter.next()
        decorations.forEach { dec ->
            if (dec.type == MapDecorationTypes.FRAME) return@forEach

            val player = playerIter.next().value

            val x = MathUtils.rescale(
                (dec.x + 126.0) * 0.5,
                mapOffsetX.toDouble(), (mapOffsetX + roomGap * floor.roomsW).toDouble(),
                0.0, floor.roomsW * 2.0
            )
            val z = MathUtils.rescale(
                (dec.y + 126.0) * 0.5,
                mapOffsetZ.toDouble(), (mapOffsetZ + roomGap * floor.roomsH).toDouble(),
                0.0, floor.roomsH * 2.0
            )
            val r = -(dec.rot / 16.0 * 360.0 + 90.0) / 180.0 * PI

            player.updatePosition(PlayerComponentPosition(x, z, r))
        }
    }

    private fun updateRooms(colors: ByteArray) {
        if (colors.size < COLOR_SIZE) return

        val visited = mutableSetOf<DungeonRoom>()
        DungeonScanner.rooms.forEachIndexed { idx, room_ ->
            if (room_ != null && !visited.add(room_)) return@forEachIndexed

            val x = idx % 6
            val z = idx / 6
            val mrx = mapOffsetX + x * roomGap
            val mrz = mapOffsetZ + z * roomGap
            val mcx = mrx + roomSize / 2 - 1
            val mcz = mrz + roomSize / 2 - 1 + 2
            val mridx = mrx + mrz * SCAN
            val mcidx = mcx + mcz * SCAN

            val roomCol = colors.getOrNull(mridx) ?: return@forEachIndexed
            val centerCol = colors.getOrNull(mcidx) ?: return@forEachIndexed

            if (roomCol == MapColors.EMPTY.color) return@forEachIndexed

            val room: DungeonRoom
            if (room_ == null) {
                val comp = ComponentPosition(x, z)
                room = DungeonRoom(mutableListOf(comp.withWorld()), 0)
                DungeonScanner.addRoom(comp, room)
            } else room = room_

            if (room.type == RoomTypes.UNKNOWN) {
                room.type = when (roomCol) {
                    MapColors.ROOM_ENTRANCE.color -> RoomTypes.ENTRANCE
                    MapColors.ROOM_BLOOD.color -> RoomTypes.BLOOD
                    MapColors.ROOM_UNOPENED.color -> RoomTypes.NORMAL
                    MapColors.ROOM_BOSS.color -> RoomTypes.YELLOW
                    MapColors.ROOM_FAIRY.color -> RoomTypes.FAIRY
                    MapColors.ROOM_NORMAL.color -> RoomTypes.NORMAL
                    MapColors.ROOM_PUZZLE.color -> RoomTypes.PUZZLE
                    MapColors.ROOM_TRAP.color -> RoomTypes.TRAP
                    else -> RoomTypes.UNKNOWN
                }
            }
            room.explored = roomCol != MapColors.ROOM_UNOPENED.color

            room.checkmark = if (roomCol == centerCol) CheckmarkTypes.NONE
            else when (centerCol) {
                MapColors.CHECK_WHITE.color -> CheckmarkTypes.WHITE
                MapColors.CHECK_GREEN.color -> CheckmarkTypes.GREEN
                MapColors.CHECK_FAIL.color -> CheckmarkTypes.FAILED
                MapColors.CHECK_UNKNOWN.color -> CheckmarkTypes.UNEXPLORED
                else -> CheckmarkTypes.NONE
            }
        }

        unscannedDoors.removeIf { comp ->
            val idx = comp.getDoorIdx()
            val mjx = mapOffsetX + (comp.x / 2) * roomGap + (comp.x and 1) * roomSize
            val mjz = mapOffsetZ + (comp.z / 2) * roomGap + (comp.z and 1) * roomSize
            val mdx = mjx + (comp.z and 1) * roomSize / 2
            val mdz = mjz + (comp.x and 1) * roomSize / 2
            val mjidx = mjx + mjz * SCAN
            val mdidx = mdx + mdz * SCAN

            val joinedCol = colors.getOrNull(mjidx) ?: return@removeIf false
            val doorCol = colors.getOrNull(mdidx) ?: return@removeIf false

            if (doorCol == MapColors.EMPTY.color) return@removeIf false

            if (joinedCol == doorCol) {
                DungeonScanner.doors[idx]?.also { d ->
                    d.rooms.forEach { it.doors.remove(d) }
                }
                DungeonScanner.doors[idx] = null
                val rooms = comp.getNeighboringRooms()
                if (rooms.size != 2) return@removeIf false
                return@removeIf DungeonScanner.mergeRooms(rooms[0], rooms[1])
            }

            val door = DungeonScanner.doors[idx] ?: let {
                val d = DungeonDoor(comp.withWorld())
                DungeonScanner.addDoor(d)
                d
            }

            return@removeIf when (doorCol) {
                MapColors.DOOR_WITHER.color -> {
                    door.type = DoorTypes.WITHER
                    door.opened = false
                    if (
                        comp.getNeighboringRooms()
                            .mapNotNull { DungeonScanner.rooms[it.getRoomIdx()] }
                            .any { it.type == RoomTypes.FAIRY && !it.explored }
                    ) door.holyShitFairyDoorPleaseStopFlashingSobs = true
                    false
                }

                MapColors.DOOR_BLOOD.color -> {
                    door.type = DoorTypes.BLOOD
                    // door.opened = false
                    // false
                    true
                }

                else -> {
                    door.type = DoorTypes.NORMAL
                    door.opened = true
                    true
                }
            }
        }
    }

    init {
        EventBus.on<PacketReceivedEvent> { event ->
            if (Location.area != "catacombs") return@on
            val packet = event.packet
            if (packet !is ClientboundMapItemDataPacket) return@on
            val mapId = packet.mapId
            if (mapId.id and 1000 != 0) return@on
            if (Dungeons.inBoss.value) return@on

            val mapState = MapItem.getSavedData(mapId, Devonian.minecraft.level) ?: return@on
            val colors = mapState.colors ?: return@on

            if (roomSize == -1 && !scanMapDimensions(colors)) return@on
            Scheduler.scheduleTask {
                updatePlayerIcons(mapState)
                updateRooms(colors)

                DungeonMap.redrawMap(
                    DungeonScanner.rooms.toList(),
                    DungeonScanner.doors.toList()
                )
            }
        }
    }
}