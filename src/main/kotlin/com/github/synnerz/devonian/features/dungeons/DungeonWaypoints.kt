package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.barrl.Context
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.dungeon.DungeonEvent
import com.github.synnerz.devonian.api.dungeon.DungeonScanner
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.features.Feature
import com.google.gson.Gson
import java.awt.Color
import kotlin.math.abs

object DungeonWaypoints : Feature(
    "dungeonWaypoints",
    "Highlights chest/items/bat spots where they would spawn at",
    "Dungeons",
    "catacombs"
) {
    private val SETTING_DISPLAY_TEXT = addSwitch(
        "displayText",
        "Whether to display a text at the location of the waypoint",
        "Dungeon Waypoints Text"
    )
    // TODO: mossy/pit has wrong coordinates
    private val waypointsData = Gson().fromJson(
        this::class.java.getResourceAsStream("/assets/devonian/dungeons/DungeonWaypoints.json")
            ?.bufferedReader()
            .use { it?.readText() },
        Array<WaypointsData>::class.java
    ).toList()
    private var roomName: String? = null
    private val roomWaypoints = mutableMapOf<String, MutableList<Triple<Int, Int, Int>>>(
        "chest" to mutableListOf(),
        "item" to mutableListOf(),
        "essence" to mutableListOf(),
        "bat" to mutableListOf(),
        "redstone" to mutableListOf(),
    )

    data class WaypointsData(val name: String, val waypoints: Map<String, List<List<Int>>>)

    // TODO: when room completed, black list until world load so the waypoints don't get re-added
    //  also re-add the chest if it was locked instead of fully removing it
    override fun initialize() {
        on<DungeonEvent.RoomEnter> { event ->
            val room = event.room
            if (room.name == roomName) return@on

            val currentWaypoints = waypointsData.find { it.name == room.name } ?: return@on
            currentWaypoints.waypoints.forEach {
                val k = it.key
                val v = it.value

                v.forEach { pos ->
                    val ( x, y, z ) = pos
                    val roomPos = room.fromComp(x, z) ?: return@forEach
                    roomWaypoints[k]?.add(Triple(roomPos.first, y, roomPos.second))
                }
            }

            roomName = room.name
        }

        on<DungeonEvent.SecretClicked> { event ->
            Scheduler.scheduleTask {
                // TODO: use chat msg for redstone key
                // "You found a Secret Redstone Key!"
                val key = when {
                    event.isSkull && event.isRedstone -> "redstone"
                    event.isSkull -> "essence"
                    else -> "chest"
                }
                roomWaypoints[key]?.removeIf {
                    it.first == event.x.toInt() && it.second == event.y.toInt() && it.third == event.z.toInt()
                }
            }
        }

        on<DungeonEvent.SecretPickup> { event ->
            Scheduler.scheduleTask {
                roomWaypoints["item"]?.removeIf {
                    abs(it.first - event.x.toInt()) + abs(it.third - event.z.toInt()) < 8
                }
            }
        }

        on<DungeonEvent.SecretBat> { event ->
            Scheduler.scheduleTask {
                roomWaypoints["bat"]?.removeIf {
                    abs(it.first - event.x.toInt()) + abs(it.third - event.z.toInt()) < 8
                }
            }
        }

        on<DungeonEvent.RoomLeave> {
            if (it.room?.name == roomName) return@on
            roomName = null
            roomWaypoints.forEach { it.value.clear() }
        }

        on<RenderWorldEvent> {
            val currentRoom = DungeonScanner.currentRoom ?: return@on
            if (roomName != currentRoom.name) return@on

            for (data in roomWaypoints) {
                val color = when (data.key) {
                    // TODO: make this customizable
                    "chest" -> Color(0, 255, 0, 255)
                    "item" -> Color(0, 0, 255, 255)
                    "essence" -> Color(255, 0, 255, 255)
                    "bat" -> Color(0, 255, 150, 255)
                    "redstone" -> Color(255, 0, 0, 255)
                    else -> Color.YELLOW
                }
                data.value.forEach { pos ->
                    Context.Immediate?.renderBox(
                        pos.first.toDouble(), pos.second.toDouble(), pos.third.toDouble(),
                        color,
                        phase = true
                    )
                    if (SETTING_DISPLAY_TEXT.get()) {
                        Context.Immediate?.renderString(
                            data.key,
                            pos.first + 0.5, pos.second + 1.0, pos.third + 0.5,
                            increase = true,
                            phase = true
                        )
                    }
                }
            }
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        roomName = null
        roomWaypoints.forEach { it.value.clear() }
    }
}