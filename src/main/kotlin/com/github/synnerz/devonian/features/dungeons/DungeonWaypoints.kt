package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.barrl.Context
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.dungeon.DungeonEvent
import com.github.synnerz.devonian.api.dungeon.DungeonScanner
import com.github.synnerz.devonian.api.events.ChatEvent
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
    private val waypointsData = Gson().fromJson(
        this::class.java.getResourceAsStream("/assets/devonian/dungeons/DungeonWaypoints.json")
            ?.bufferedReader()
            .use { it?.readText() },
        Array<WaypointsData>::class.java
    ).toList()
    private var roomName: String? = null
    private val waypoints = mutableMapOf<String, MutableMap<String, MutableList<Triple<Int, Int, Int>>>>()
    private val currentWaypoint get() = waypoints[roomName]

    data class WaypointsData(val name: String, val waypoints: Map<String, List<List<Int>>>)

    override fun initialize() {
        on<DungeonEvent.RoomEnter> { event ->
            val room = event.room
            if (room.name == null) return@on
            if (waypoints.containsKey(room.name)) {
                roomName = room.name
                return@on
            }

            val currentWaypoints = waypointsData.find { it.name == room.name } ?: return@on
            currentWaypoints.waypoints.forEach {
                val k = it.key
                val v = it.value

                v.forEach { pos ->
                    val ( x, y, z ) = pos
                    val roomPos = room.fromComp(x, z) ?: return@forEach
                    waypoints
                        .getOrPut(room.name!!) { mutableMapOf() }
                        .getOrPut(k) { mutableListOf() }
                        .add(Triple(roomPos.first, y, roomPos.second))
                }
            }

            roomName = room.name
        }

        on<ChatEvent> { event ->
            event.matches("^You found a Secret Redstone Key!$".toRegex()) ?: return@on

            val player = minecraft.player ?: return@on
            val x = player.x
            val y = player.y
            val z = player.z

            currentWaypoint?.get("redstone")?.removeIf {
                it.first == x.toInt() && it.second == y.toInt() && it.third == z.toInt()
            }
        }

        on<DungeonEvent.SecretClicked> { event ->
            Scheduler.scheduleTask {
                val key = when {
                    event.isSkull && event.isRedstone -> "redstone"
                    event.isSkull -> "essence"
                    else -> "chest"
                }
                currentWaypoint?.get(key)?.removeIf {
                    it.first == event.x.toInt() && it.second == event.y.toInt() && it.third == event.z.toInt()
                }
            }
        }

        on<DungeonEvent.SecretPickup> { event ->
            Scheduler.scheduleTask {
                currentWaypoint?.get("item")?.removeIf {
                    abs(it.first - event.x.toInt()) + abs(it.third - event.z.toInt()) < 8
                }
            }
        }

        on<DungeonEvent.SecretBat> { event ->
            Scheduler.scheduleTask {
                currentWaypoint?.get("bat")?.removeIf {
                    abs(it.first - event.x.toInt()) + abs(it.third - event.z.toInt()) < 10
                }
            }
        }

        on<DungeonEvent.RoomLeave> {
            roomName = null
        }

        on<RenderWorldEvent> {
            val currentRoom = DungeonScanner.currentRoom ?: return@on
            if (roomName != currentRoom.name) return@on

            currentWaypoint ?: return@on
            for (data in currentWaypoint!!) {
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
        waypoints.clear()
    }
}