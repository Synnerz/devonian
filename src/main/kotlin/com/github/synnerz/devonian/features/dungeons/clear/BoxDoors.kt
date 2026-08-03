package com.github.synnerz.devonian.features.dungeons.clear

import com.github.synnerz.devonian.api.dungeon.DungeonRoom
import com.github.synnerz.devonian.api.dungeon.DungeonScanner
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.dungeon.WorldComponentPosition
import com.github.synnerz.devonian.api.dungeon.mapEnums.CheckmarkTypes
import com.github.synnerz.devonian.api.dungeon.mapEnums.DoorTypes
import com.github.synnerz.devonian.api.dungeon.mapEnums.RoomTypes
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.render.Render3DImmediate
import java.awt.Color
import java.util.*
import kotlin.math.min

object BoxDoors : Feature(
    "boxDoors",
    "Draws boxes around doors in dungeons.",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "Highlights",
    searchTags = setOf("wither", "blood", "highlight"),
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.Clear.isActiveState)
    }

    private val SETTING_DOOR_LOCKED_WIRE_COLOR = addColorPicker(
        "lockedWireColor",
        Color(255, 0, 0).rgb,
        "",
        "Locked Door Outline Color",
    )
    private val SETTING_DOOR_LOCKED_FILL_COLOR = addColorPicker(
        "lockedFillColor",
        Color(255, 0, 0, 64).rgb,
        "",
        "Locked Door Fill Color",
    )
    private val SETTING_DOOR_KEY_WIRE_COLOR = addColorPicker(
        "keyWireColor",
        Color(0, 255, 0).rgb,
        "",
        "Unlocked Door Outline Color",
    )
    private val SETTING_DOOR_KEY_FILL_COLOR = addColorPicker(
        "keyFillColor",
        Color(0, 255, 0, 64).rgb,
        "",
        "Unlocked Door Fill Color",
    )
    private val SETTING_RENDER_NORMAL_DOORS = addSwitch(
        "renderNormalDoors",
        true,
        "Highlights normal doorways, not only the wither/blood ones",
        "Highlight Normal Doors",
    )
    private val SETTING_HIDE_NORMAL_DOOR_GREEN = addSwitch(
        "hideNormalDoorGreen",
        true,
        "Don't box normal doors when the room it leads to is not useful." +
        "Currently, a room is 'useful' if it does not have a green check or leads to a room without a green check.",
        "Hide Useless Doors",
    )
    private val SETTING_DOOR_NORMAL_WIRE_COLOR = addColorPicker(
        "normalWireColor",
        Color(0, 128, 128).rgb,
        "",
        "Normal Door Outline Color",
    )
    private val SETTING_DOOR_NORMAL_FILL_COLOR = addColorPicker(
        "normalFillColor",
        Color(0, 128, 128, 0).rgb,
        "",
        "Normal Door Fill Color",
    )
    private val SETTING_DOOR_LINE_WIDTH = addSlider(
        "doorLineWidth",
        3.0,
        1.0, 10.0,
        "Line width of the box outline of the door",
        "Door Line Width",
    )
    private val SETTING_DOOR_FILL_PHASE = addSwitch(
        "fillPhase",
        false,
        "",
        "Door Fill Phase",
    )
    private val SETTING_RENDER_HIDDEN_DOORS = addSwitch(
        "renderUnknownDoors",
        false,
        "Whether to highlight the doors that are in rooms that have not been explored yet." +
        "Only applies to wither/blood doors.",
        "Highlight Unknown Doors",
        cheeto = true,
    )
    private val SETTING_OUTLINE_OPENED_BLOOD = addSwitch(
        "outlineOpenedBlood",
        false,
        "Adds an outline to blood room whenever it's opened",
        "Outline Opened Blood",
    )

    private val EMPTY_COLOR = Color(0, 0, 0, 0)
    private var witherKeys = 0
    private var bloodKey = false
    private var bloodOpened = false

    private val witherKeyRegex = "^.+?(\\w+) has obtained Wither Key!$".toRegex()
    private val bloodKeyRegex = "^.+?(\\w+) has obtained Blood Key!$".toRegex()
    private val witherDoorRegex = "^(\\w+) opened a WITHER door!$".toRegex()

    override fun initialize() {
        on<ChatEvent> { event ->
            var b = true
            when (event.message) {
                "A Wither Key was picked up!" -> witherKeys++
                "A Blood Key was picked up!" -> bloodKey = true
                "The BLOOD DOOR has been opened!" -> {
                    bloodKey = false
                    bloodOpened = true
                }
                else -> b = false
            }
            if (b) return@on

            var match = event.matches(witherKeyRegex)
            if (match != null) {
                witherKeys++
                return@on
            }
            match = event.matches(bloodKeyRegex)
            if (match != null) {
                bloodKey = true
                return@on
            }
            match = event.matches(witherDoorRegex)
            if (match != null) {
                witherKeys = min(witherKeys - 1, 0)
                return@on
            }
        }

        on<RenderWorldEvent> {
            DungeonScanner.doors.forEach {
                if (it == null) return@forEach
                if (it.wasBlood && SETTING_OUTLINE_OPENED_BLOOD.get() && bloodOpened) {
                    drawDoor(it.comp, SETTING_DOOR_KEY_WIRE_COLOR.getColor(), EMPTY_COLOR)
                    return@forEach
                }

                val hasKey = when (it.type) {
                    DoorTypes.NORMAL,
                    DoorTypes.ENTRANCE
                        -> {
                        if (it.rooms.any { it.type == RoomTypes.FAIRY && !it.explored }) witherKeys > 0
                        else return@forEach
                    }

                    DoorTypes.WITHER -> witherKeys > 0
                    DoorTypes.BLOOD -> bloodKey
                }

                if (it.opened && !it.holyShitFairyDoorPleaseStopFlashingSobs) return@forEach

                val hasExplored = it.rooms.all { !it.explored }
                if (!SETTING_RENDER_HIDDEN_DOORS.get() && hasExplored) return@forEach

                val colorWire: Color
                val colorFill: Color
                if (hasKey && !hasExplored) {
                    colorWire = SETTING_DOOR_KEY_WIRE_COLOR.getColor()
                    colorFill = SETTING_DOOR_KEY_FILL_COLOR.getColor()
                } else {
                    colorWire = SETTING_DOOR_LOCKED_WIRE_COLOR.getColor()
                    colorFill = SETTING_DOOR_LOCKED_FILL_COLOR.getColor()
                }

                drawDoor(it.comp, colorWire, colorFill)
            }

            if (SETTING_RENDER_NORMAL_DOORS.get()) {
                val room = DungeonScanner.currentRoom ?: return@on
                room.doors.forEach {
                    if (it.type !== DoorTypes.NORMAL && it.type !== DoorTypes.ENTRANCE) return@forEach
                    if (
                        SETTING_HIDE_NORMAL_DOOR_GREEN.get() &&
                        it.rooms.all {
                            it === room ||
                            !isRoomUseful(
                                it,
                                Collections.newSetFromMap<DungeonRoom>(IdentityHashMap())
                                    .also { it.add(room) }
                            )
                        }
                    ) return@forEach

                    drawDoor(
                        it.comp,
                        SETTING_DOOR_NORMAL_WIRE_COLOR.getColor(),
                        SETTING_DOOR_NORMAL_FILL_COLOR.getColor()
                    )
                }
            }
        }
    }

    private fun drawDoor(comp: WorldComponentPosition, wire: Color, fill: Color) {
        Render3DImmediate.renderWireframeBox(
            comp.wx + 0.5, 69.0, comp.wz + 0.5,
            3.0, 4.0,
            wire,
            SETTING_DOOR_LINE_WIDTH.get(),
            true,
            centered = true,
        )
        Render3DImmediate.renderFilledBox(
            comp.wx + 0.5, 69.0, comp.wz + 0.5,
            3.0, 4.0,
            fill,
            SETTING_DOOR_FILL_PHASE.get(),
            centered = true,
        )
    }

    private fun isRoomUseful(room: DungeonRoom, visited: MutableSet<DungeonRoom>): Boolean {
        if (room.type == RoomTypes.ENTRANCE) return false
        if (room.checkmark !== CheckmarkTypes.GREEN) return true
        return room.doors.any {
            it.rooms.any {
                it !== room &&
                visited.add(it) &&
                isRoomUseful(it, visited)
            }
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        witherKeys = 0
        bloodKey = false
        bloodOpened = false
    }
}