package com.github.synnerz.devonian.api.dungeon

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.dungeon.mapEnums.CheckmarkTypes
import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.api.events.PacketReceivedEvent
import com.github.synnerz.devonian.features.dungeons.map.DungeonMap
import net.minecraft.item.FilledMapItem
import net.minecraft.item.map.MapState
import net.minecraft.network.packet.s2c.play.MapUpdateS2CPacket

object DungeonMapScanner {
    private const val COLOR_SIZE = 16384
    var roomSize = -1
    var gapSize = -1
    var corners = mutableListOf<Int>()
    val playerIcons = mutableMapOf<String, PlayerMapData>()
    val playersInWorld = mutableListOf<PlayerWorldData>()

    data class PlayerMapData(val x: Int, val z: Int, val rotation: Int, val name: String)
    data class PlayerWorldData(
        var iconX: Int,
        var iconZ: Int,
        var worldX: Int,
        var worldZ: Int,
        var rotation: Int,
        val name: String,
        var inRender: Boolean = false
        // add current room probably ?
    )

    private fun findCorner(colors: ByteArray) {
        var pixelIdx = -1

        colors.forEachIndexed { index, byte ->
            if (
                byte == 30.toByte() &&
                index + 15 < colors.size &&
                colors[index + 15] == 30.toByte() &&
                index + 128 * 15 < colors.size &&
                colors[index + 15 * 128] == 30.toByte()
            ) {
                pixelIdx = index
                return@forEachIndexed
            }
        }

        if (pixelIdx == -1) return

        var idx = 0

        while (colors[pixelIdx + idx] == 30.toByte()) idx++

        roomSize = idx
        gapSize = idx + 4
        corners = mutableListOf((pixelIdx % 128) % gapSize, (pixelIdx / 128) % gapSize)
    }

    private fun updatePlayerIcons(mapState: MapState) {
        val decorations = mapState.decorations
        decorations.forEachIndexed { index, mapDecoration ->
            if (index > Dungeons.partyMembers.size - 1) return@forEachIndexed
            // TODO: filter out dead players as well as the (current) client player
            val name = Dungeons.partyMembers[index]
            val x = mapDecoration.x + 128
            val z = mapDecoration.z + 128
            val rotation = (mapDecoration.rotation * 360) / 16 + 180

            playerIcons[name] = PlayerMapData(x, z, rotation, name)
        }

        updatePlayerData()
    }

    private fun clamp(
        n: Int,
        inMin: Int,
        inMax: Int,
        outMin: Int,
        outMax: Int
    ): Int {
        if (n <= inMin) return outMin
        if (n >= inMax) return outMax

        return (n - inMin) * (outMax - outMin) / (inMax - inMin) + outMin
    }

    private fun updatePlayerData() {
        playerIcons.forEach { (k, v) ->
            val worldData = playersInWorld.find { it.name == k } ?: return@forEach
            worldData.iconX = clamp(v.x / 2 - corners[0], 0, roomSize * 6 + 20, 0, DungeonScanner.defaultMapSize.x)
            worldData.iconZ = clamp(v.z / 2 - corners[1], 0, roomSize * 6 + 20, 0, DungeonScanner.defaultMapSize.z)
            worldData.worldX = clamp(worldData.iconX, 0, 125, -200, -10)
            worldData.worldZ = clamp(worldData.iconZ, 0, 125, -200, -10)
            worldData.rotation = v.rotation
        }
    }

    private fun onPlayerMove(data: PlayerWorldData, x: Int, z: Int, yaw: Float) {
        if (x !in -200..-10 || z !in -200..-10) return

        data.inRender = true
        data.iconX = clamp(x, -200, -10, 0, DungeonScanner.defaultMapSize.x)
        data.iconZ = clamp(z, -200, -10, 0, DungeonScanner.defaultMapSize.z)
        data.worldX = x
        data.worldZ = z
        data.rotation = (yaw + 180f).toInt()
    }

    fun checkPlayerState() {
        if (playersInWorld.size == Dungeons.partyMembers.size) {
            for (player in playersInWorld) {
                val entity = Devonian.minecraft.world?.players?.find { it.name?.string == player.name } ?: continue
                val ping = Devonian.minecraft.networkHandler?.getPlayerListEntry(entity.uuid)?.latency ?: -1
                if (ping != -1) onPlayerMove(player, entity.x.toInt(), entity.z.toInt(), entity.yaw)
                else player.inRender = false
            }
            return
        }

        for (player in Dungeons.partyMembers) {
            if (playersInWorld.find { it.name == player } != null) continue
            val entity = Devonian.minecraft.world?.players?.find { it.name?.string == player } ?: continue
            val ping = Devonian.minecraft.networkHandler?.getPlayerListEntry(entity.uuid)?.latency ?: continue
            if (ping == -1) continue

            playersInWorld.add(
                PlayerWorldData(-1, -1, -1, -1, -1, player)
            )
        }
    }

    init {
        EventBus.on<PacketReceivedEvent> { event ->
            val packet = event.packet
            if (packet !is MapUpdateS2CPacket) return@on
            val mapId = packet.mapId
            if (mapId.id and 1000 != 0) return@on

            val mapState = FilledMapItem.getMapState(mapId, Devonian.minecraft.world) ?: return@on
            val colors = mapState.colors ?: return@on

            if (corners.isEmpty()) findCorner(colors)
            Scheduler.scheduleTask {
                updatePlayerIcons(mapState)
            }

            if (colors.size < COLOR_SIZE) return@on

            for (room in DungeonScanner.rooms) {
                if (room == null || room.comps.isEmpty()) continue
                val leftMost = room.comps.firstOrNull() ?: continue
                val x = leftMost.cx / 2
                val z = leftMost.cz / 2

                val mapX = corners[0] + roomSize / 2 + gapSize * x
                val mapZ = corners[1] + roomSize / 2 + 1 + gapSize * z
                val idx = mapX + mapZ * 128
                if (idx - 1 > COLOR_SIZE) continue

                val centerColor = colors[idx - 1]
                val roomIdx = idx + 5 + 128 * 4
                if (roomIdx > COLOR_SIZE) continue
                val roomColor = colors[roomIdx]

                if (roomColor == 0.toByte() || roomColor == 85.toByte()) {
                    room.explored = false
                    continue
                }

                room.explored = true

                val check = when {
                    centerColor == 30.toByte() && roomColor != 30.toByte() -> CheckmarkTypes.GREEN
                    centerColor == 34.toByte() -> CheckmarkTypes.WHITE
                    centerColor == 18.toByte() && roomColor != 18.toByte() -> CheckmarkTypes.FAILED
                    room.checkmark == CheckmarkTypes.UNEXPLORED -> CheckmarkTypes.NONE
                    else -> null
                } ?: continue

                room.checkmark = check
            }

            DungeonMap.redrawMap(
                DungeonScanner.rooms.toList(),
                DungeonScanner.doors.toList()
            )
        }
    }
}