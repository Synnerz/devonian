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
    private val SETTING_CHEST_OUTLINE = addColorPicker(
        "chestOutline",
        "The color of the highlight outline for chest waypoints",
        "Dungeon Waypoints Chest Outline",
        Color(0, 255, 0, 255).rgb
    )
    private val SETTING_CHEST_FILLED = addColorPicker(
        "chestFilled",
        "The color of the highlight filled for chest waypoints",
        "Dungeon Waypoints Chest Filled",
        Color(0, 255, 0, 80).rgb
    )
    private val SETTING_ITEM_OUTLINE = addColorPicker(
        "itemOutline",
        "The color of the highlight outline for item waypoints",
        "Dungeon Waypoints Item Outline",
        Color(0, 0, 255, 255).rgb
    )
    private val SETTING_ITEM_FILLED = addColorPicker(
        "itemFilled",
        "The color of the highlight filled for item waypoints",
        "Dungeon Waypoints Item Filled",
        Color(0, 0, 255, 80).rgb
    )
    private val SETTING_ESSENCE_OUTLINE = addColorPicker(
        "essenceOutline",
        "The color of the highlight outline for essence waypoints",
        "Dungeon Waypoints Essence Outline",
        Color(255, 0, 255, 255).rgb
    )
    private val SETTING_ESSENCE_FILLED = addColorPicker(
        "essenceFilled",
        "The color of the highlight filled for essence waypoints",
        "Dungeon Waypoints Essence Filled",
        Color(255, 0, 255, 80).rgb
    )
    private val SETTING_BAT_OUTLINE = addColorPicker(
        "batOutline",
        "The color of the highlight outline for bat waypoints",
        "Dungeon Waypoints Bat Outline",
        Color(0, 255, 150, 255).rgb
    )
    private val SETTING_BAT_FILLED = addColorPicker(
        "batFilled",
        "The color of the highlight filled for bat waypoints",
        "Dungeon Waypoints Bat Filled",
        Color(0, 255, 150, 80).rgb
    )
    private val SETTING_REDSTONE_OUTLINE = addColorPicker(
        "redstoneOutline",
        "The color of the highlight outline for redstone key waypoints",
        "Dungeon Waypoints Redstone Outline",
        Color(255, 0, 0, 255).rgb
    )
    private val SETTING_REDSTONE_FILLED = addColorPicker(
        "redstoneFilled",
        "The color of the highlight filled for redstone key waypoints",
        "Dungeon Waypoints Redstone Filled",
        Color(255, 0, 0, 80).rgb
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
                val outlineColor = when (data.key) {
                    "chest" -> SETTING_CHEST_OUTLINE.getColor()
                    "item" -> SETTING_ITEM_OUTLINE.getColor()
                    "essence" -> SETTING_ESSENCE_OUTLINE.getColor()
                    "bat" -> SETTING_BAT_OUTLINE.getColor()
                    "redstone" -> SETTING_REDSTONE_OUTLINE.getColor()
                    else -> Color.YELLOW
                }
                val filledColor = when (data.key) {
                    "chest" -> SETTING_CHEST_FILLED.getColor()
                    "item" -> SETTING_ITEM_FILLED.getColor()
                    "essence" -> SETTING_ESSENCE_FILLED.getColor()
                    "bat" -> SETTING_BAT_FILLED.getColor()
                    "redstone" -> SETTING_REDSTONE_FILLED.getColor()
                    else -> Color.YELLOW
                }
                data.value.forEach { pos ->
                    Context.Immediate?.renderBox(
                        pos.first.toDouble(), pos.second.toDouble(), pos.third.toDouble(),
                        outlineColor,
                        phase = true
                    )
                    Context.Immediate?.renderFilledBox(
                        pos.first.toDouble(), pos.second.toDouble(), pos.third.toDouble(),
                        filledColor,
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