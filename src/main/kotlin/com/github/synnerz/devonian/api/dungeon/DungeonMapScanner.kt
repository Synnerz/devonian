package com.github.synnerz.devonian.api.dungeon

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.dungeon.mapEnums.CheckmarkTypes
import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.api.events.PacketReceivedEvent
import com.github.synnerz.devonian.features.dungeons.map.DungeonMap
import com.github.synnerz.devonian.utils.math.MathUtils
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket
import net.minecraft.world.item.MapItem
import net.minecraft.world.level.saveddata.maps.MapDecorationType
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

    fun reset() {
        roomSize = -1
        roomGap = -1
        roomCount = -1
        mapOffsetX - 1
        mapOffsetZ - 1
        mapWidth = -1
        mapHeight = -1
    }

    private enum class MapColors(val color: Byte) {
        EMPTY(0),

        CHECK_WHITE(34),
        CHECK_GREEN(30),
        CHECK_FAIL(18),

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

    init {
        EventBus.on<PacketReceivedEvent> { event ->
            val packet = event.packet
            if (packet !is ClientboundMapItemDataPacket) return@on
            val mapId = packet.mapId
            if (mapId.id and 1000 != 0) return@on

            val mapState = MapItem.getSavedData(mapId, Devonian.minecraft.level) ?: return@on
            val colors = mapState.colors ?: return@on

            if (roomSize == -1 && !scanMapDimensions(colors)) return@on
            Scheduler.scheduleTask {
                updatePlayerIcons(mapState)

                DungeonMap.redrawMap(
                    DungeonScanner.rooms.toList(),
                    DungeonScanner.doors.toList()
                )
            }

            if (colors.size < COLOR_SIZE) return@on

            for (room in DungeonScanner.rooms) {
                if (room == null || room.comps.isEmpty()) continue
                val leftMost = room.comps.firstOrNull() ?: continue
                val x = leftMost.cx / 2
                val z = leftMost.cz / 2

                val mapX = mapOffsetX + roomSize / 2 + roomGap * x
                val mapZ = mapOffsetZ + roomSize / 2 + 1 + roomGap * z
                val idx = mapX + mapZ * SCAN
                if (idx - 1 > COLOR_SIZE) continue

                val centerColor = colors[idx - 1]
                val roomIdx = idx + 5 + SCAN * 4
                if (roomIdx > COLOR_SIZE) continue
                val roomColor = colors[roomIdx]

                if (roomColor == MapColors.EMPTY.color || roomColor == MapColors.ROOM_UNOPENED.color) {
                    room.explored = false
                    continue
                }

                room.explored = true

                val check = when {
                    centerColor == MapColors.CHECK_GREEN.color && roomColor != MapColors.ROOM_ENTRANCE.color
                        -> CheckmarkTypes.GREEN

                    centerColor == MapColors.CHECK_WHITE.color -> CheckmarkTypes.WHITE
                    centerColor == MapColors.CHECK_FAIL.color && roomColor != MapColors.ROOM_BLOOD.color
                        -> CheckmarkTypes.FAILED

                    room.checkmark == CheckmarkTypes.UNEXPLORED -> CheckmarkTypes.NONE
                    else -> null
                } ?: continue

                room.checkmark = check
            }
        }
    }
}