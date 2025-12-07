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
import java.util.EnumMap
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
        Array<WaypointsDataJSON>::class.java
    ).toList().map { WaypointsData(it) }.toTypedArray()
    private var roomID: Int? = null
    private val waypoints = arrayOfNulls<MutableMap<WaypointType, MutableList<IntTriple>>?>(waypointsData.maxOf { it.roomID } + 1)

    private fun getWaypoints(id: Int? = roomID): MutableMap<WaypointType, MutableList<IntTriple>>? {
        val id = roomID ?: return null
        return waypoints.getOrNull(id)
    }

    enum class WaypointType(val key: String) {
        CHEST("chest"),
        ITEM("item"),
        ESSENCE("essence"),
        BAT("redstone"),
        REDSTONE("redstone"),
        UNKNOWN("unknown");

        companion object {
            fun from(key: String) = entries.find { it.key == key } ?: UNKNOWN
        }
    }
    
    data class IntTriple(val x: Int, val y: Int, val z: Int)

    data class WaypointsDataJSON(val name: String, val waypoints: Map<String, List<List<Int>>>, val roomID: Int)
    data class WaypointsData(val waypoints: Map<WaypointType, List<IntTriple>>, val roomID: Int) {
        constructor(old: WaypointsDataJSON) : this(
            EnumMap<WaypointType, List<IntTriple>>(
                old.waypoints.entries.associate { (k, v) ->
                    WaypointType.from(k) to v.map { IntTriple(it[0], it[1], it[2]) }
                }
            ),
            old.roomID
        )
    }

    override fun initialize() {
        on<DungeonEvent.RoomEnter> { event ->
            val room = event.room
            val id = room.roomID ?: return@on
            roomID = id

            if (getWaypoints(id) != null) return@on

            val waypointData = waypointsData.getOrNull(id) ?: return@on
            val currentWaypoints = waypoints.getOrElse(id) {
                waypoints[id] = EnumMap(WaypointType::class.java)
                waypoints[id]
            } ?: return@on
            waypointData.waypoints.forEach {
                val k = it.key
                val v = it.value

                v.forEach { pos ->
                    val roomPos = room.fromComp(pos.x, pos.z) ?: return@forEach

                    currentWaypoints
                        .getOrPut(k) { mutableListOf() }
                        .add(IntTriple(roomPos.first, pos.y, roomPos.second))
                }
            }
        }

        on<ChatEvent> { event ->
            // TODO: later on impl chest locked re-add "That chest is locked!"
            val player = minecraft.player ?: return@on
            val x = player.x
            val z = player.z

            event.matches("^You found a Secret Redstone Key!$".toRegex()) ?: return@on

            Scheduler.scheduleTask {
                getWaypoints()?.get(WaypointType.REDSTONE)?.removeIf {
                    abs(it.x - x.toInt()) + abs(it.z - z.toInt()) < 15
                }
            }
        }

        on<DungeonEvent.SecretClicked> { event ->
            Scheduler.scheduleTask {
                val key = when {
                    event.isSkull && event.isRedstone -> WaypointType.REDSTONE
                    event.isSkull -> WaypointType.ESSENCE
                    else -> WaypointType.CHEST
                }
                getWaypoints()?.get(key)?.removeIf {
                    it.x == event.x.toInt() && it.y == event.y.toInt() && it.z == event.z.toInt()
                }
            }
        }

        on<DungeonEvent.SecretPickup> { event ->
            Scheduler.scheduleTask {
                getWaypoints()?.get(WaypointType.ITEM)?.removeIf {
                    abs(it.x - event.x.toInt()) + abs(it.z - event.z.toInt()) < 8
                }
            }
        }

        on<DungeonEvent.SecretBat> { event ->
            Scheduler.scheduleTask {
                getWaypoints()?.get(WaypointType.BAT)?.removeIf {
                    abs(it.x - event.x.toInt()) + abs(it.z - event.z.toInt()) < 10
                }
            }
        }

        on<DungeonEvent.RoomLeave> {
            roomID = null
        }

        on<RenderWorldEvent> {
            val id = roomID ?: return@on
            val currentRoom = DungeonScanner.currentRoom ?: return@on
            if (id != currentRoom.roomID) return@on

            val waypoints = getWaypoints(id) ?: return@on
            for (data in waypoints) {
                val outlineColor = when (data.key) {
                    WaypointType.CHEST -> SETTING_CHEST_OUTLINE.getColor()
                    WaypointType.ITEM -> SETTING_ITEM_OUTLINE.getColor()
                    WaypointType.ESSENCE -> SETTING_ESSENCE_OUTLINE.getColor()
                    WaypointType.BAT -> SETTING_BAT_OUTLINE.getColor()
                    WaypointType.REDSTONE -> SETTING_REDSTONE_OUTLINE.getColor()
                    WaypointType.UNKNOWN -> SETTING_REDSTONE_OUTLINE.getColor()
                }
                val filledColor = when (data.key) {
                    WaypointType.CHEST -> SETTING_CHEST_FILLED.getColor()
                    WaypointType.ITEM -> SETTING_ITEM_FILLED.getColor()
                    WaypointType.ESSENCE -> SETTING_ESSENCE_FILLED.getColor()
                    WaypointType.BAT -> SETTING_BAT_FILLED.getColor()
                    WaypointType.REDSTONE -> SETTING_REDSTONE_FILLED.getColor()
                    WaypointType.UNKNOWN -> Color.YELLOW
                }
                data.value.forEach { pos ->
                    Context.Immediate?.renderBox(
                        pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(),
                        outlineColor,
                        phase = true
                    )
                    Context.Immediate?.renderFilledBox(
                        pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(),
                        filledColor,
                        phase = true
                    )
                    if (SETTING_DISPLAY_TEXT.get()) {
                        Context.Immediate?.renderString(
                            data.key.key.replaceFirstChar { it.uppercaseChar() },
                            pos.x + 0.5, pos.y + 1.0, pos.z + 0.5,
                            increase = true,
                            phase = true
                        )
                    }
                }
            }
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        roomID = null
        waypoints.fill(null)
    }
}