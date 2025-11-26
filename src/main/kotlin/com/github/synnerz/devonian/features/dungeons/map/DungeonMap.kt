package com.github.synnerz.devonian.features.dungeons.map

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.bufimgrenderer.BufferedImageRenderer
import com.github.synnerz.devonian.api.bufimgrenderer.BufferedImageUploader
import com.github.synnerz.devonian.api.bufimgrenderer.TexturedQuadRenderState
import com.github.synnerz.devonian.api.dungeon.*
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.hud.HudFeature
import com.github.synnerz.devonian.hud.texthud.SimpleTextHud
import com.github.synnerz.devonian.hud.texthud.TextHud
import com.github.synnerz.devonian.utils.BoundingBox
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.resources.ResourceLocation
import org.joml.Matrix3x2f
import java.awt.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object DungeonMap : HudFeature(
    "dungeonMap",
    "Dungeon Map",
    "Dungeon Map",
    "catacombs"
) {
    private val SETTING_RENDER_NAMES = addSwitch(
        "renderNames",
        "Render player names above marker",
        "Render Names",
        true
    )
    private val SETTING_RENDER_NAMES_ONLY_LEAP = addSwitch(
        "namesRequireLeap",
        "Only render player names when holding leap",
        "Render Names When Holding Leap",
        false
    )
    private val SETTING_USE_CLASS_NAME = addSwitch(
        "useClassName",
        "Render the class name instead of the player name",
        "Render Class Name",
        true
    )
    private val SETTING_COLOR_NAME_BY_CLASS = addSwitch(
        "colorNameClass",
        "Colors the player names by their respective class",
        "Color Player Names",
        true
    )
    private val SETTING_NAME_SCALE = addSlider(
        "nameScale",
        "",
        "Player Name Scale",
        0.0, 10.0,
        1.0
    )
    private val SETTING_MARKER_SCALE = addSlider(
        "markerScale",
        "",
        "Marker Scale",
        0.0, 10.0,
        1.0
    )
    private val SETTING_ROOM_ENTRANCE_COLOR = addColorPicker(
        "roomEntranceColor",
        "",
        "Entrance Room Color",
        Color(0, 123, 0).rgb
    )
    private val SETTING_ROOM_NORMAL_COLOR = addColorPicker(
        "roomNormalColor",
        "",
        "Normal Room Color",
        Color(114, 67, 27).rgb
    )
    private val SETTING_ROOM_MINIBOSS_COLOR = addColorPicker(
        "roomMinibossColor",
        "(as in: has a miniboss, not yellow)",
        "Miniboss Room Color",
        Color(114, 67, 27).rgb
    )
    private val SETTING_ROOM_FAIRY_COLOR = addColorPicker(
        "roomFairyColor",
        "",
        "Fairy Room Color",
        Color(239, 126, 163).rgb
    )
    private val SETTING_ROOM_BLOOD_COLOR = addColorPicker(
        "roomBloodColor",
        "",
        "Blood Room Color",
        Color(255, 0, 0).rgb
    )
    private val SETTING_ROOM_PUZZLE_COLOR = addColorPicker(
        "roomPuzzleColor",
        "",
        "Puzzle Room Color",
        Color(176, 75, 213).rgb
    )
    private val SETTING_ROOM_TRAP_COLOR = addColorPicker(
        "roomTrapColor",
        "",
        "Trap Room Color",
        Color(213, 126, 50).rgb
    )
    private val SETTING_ROOM_YELLOW_COLOR = addColorPicker(
        "roomYellowColor",
        "",
        "Yellow Room Color",
        Color(226, 226, 50).rgb
    )
    private val SETTING_ROOM_RARE_COLOR = addColorPicker(
        "roomRareColor",
        "",
        "Rare Room Color",
        Color(0, 67, 27).rgb
    )
    private val SETTING_ROOM_UNKNOWN_COLOR = addColorPicker(
        "roomUnknownColor",
        "",
        "Unknown Room Color",
        Color(64, 64, 64).rgb
    )
    private val SETTING_DOOR_WITHER_COLOR = addColorPicker(
        "doorWitherColor",
        "",
        "Wither Door Color",
        Color(0, 0, 0).rgb
    )
    private val SETTING_DOOR_BLOOD_COLOR = addColorPicker(
        "doorBloodColor",
        "",
        "Blood Door Color",
        Color(255, 0, 0).rgb
    )
    private val SETTING_DOOR_ENTRANCE_COLOR = addColorPicker(
        "doorEntranceColor",
        "",
        "Entrance Door Color",
        Color(0, 123, 0).rgb
    )
    private val SETTING_ROOM_SIZE = addDecimalSlider(
        "roomSize",
        "",
        "Room Size",
        0.0, 1.0,
        0.8
    )
    private val SETTING_DOOR_SIZE = addDecimalSlider(
        "doorSize",
        "",
        "Door Size",
        0.0, 1.0,
        0.4
    )
    private val SETTING_RENDER_CHECKMARK = addSwitch(
        "renderCheckmark",
        "",
        "Render Checkmarks",
        true
    )
    private val SETTING_RENDER_PUZZLE_ICON = addSwitch(
        "renderPuzzleIcon",
        "",
        "Render Puzzle Icon",
        true
    )
    private val SETTING_RENDER_ROOM_NAMES = addSwitch(
        "renderRoomNames",
        "",
        "Render Room Names",
        true
    )
    private val SETTING_RENDER_SECRET_COUNT = addSwitch(
        "renderSecretCount",
        "(we dont actually track or sync secrets right now)",
        "Render Secret Count",
        false
    )
    private val SETTING_RENDER_PUZZLE_NAME = addSwitch(
        "renderPuzzleName",
        "",
        "Render Puzzle Name",
        false
    )
    private val SETTING_ICON_SIZE = addDecimalSlider(
        "iconSize",
        "Affects puzzles + checkmarks. (% of the room)",
        "Icon Size",
        0.0, 1.0,
        0.6
    )
    private val SETTING_ICON_ALIGNMENT = addSelection(
        "iconAlign",
        "Alignment of the icon with respect to the room layout",
        "Icon Alignment",
        DungeonMapRoomInfoAlignment.entries.map { it.str },
        DungeonMapRoomInfoAlignment.Center.ordinal
    )
    private val SETTING_TEXT_SIZE = addDecimalSlider(
        "textSize",
        "Affects room names + secret count. (% of the room)",
        "Text Size",
        0.0, 1.0,
        0.8
    )
    private val SETTING_TEXT_ALIGNMENT = addSelection(
        "textAlign",
        "Alignment of the text with respect to the room layout",
        "Text Alignment",
        DungeonMapRoomInfoAlignment.entries.map { it.str },
        DungeonMapRoomInfoAlignment.TopLeft.ordinal
    )
    private val SETTING_TEXT_SHADOW = addSwitch(
        "textShadow",
        "",
        "Text Shadow",
        true
    )
    private val SETTING_COLOR_ROOM_TEXT = addSwitch(
        "colorRoomName",
        "Change color of room name based on the room checkmark",
        "Color Room Name",
        true
    )
    private val SETTING_RENDER_HIDDEN_ROOMS = addSwitch(
        "renderHiddenRooms",
        "",
        "Render Hidden Rooms",
        false,
        cheeto = true
    )
    private val SETTING_HIDDEN_ROOM_DARKEN = addDecimalSlider(
        "hiddenRoomDarken",
        "factor by which to darken hidden rooms",
        "Hidden Room Darken Factor",
        0.0, 1.0,
        0.7
    )

    private val mapRenderer = DungeonMapBaseRenderer()

    override fun getBounds(): BoundingBox = BoundingBox(
        x, y,
        100.0 * scale, 100.0 * scale
    )

    fun redrawMap(rooms: List<DungeonRoom?>, doors: List<DungeonDoor?>) {
        if (Dungeons.floor == FloorType.None) return
        val bounds = getBounds()
        val window = minecraft.window
        mapRenderer.update(
            (bounds.w * window.guiScale + 0.5).toInt(),
            (bounds.h * window.guiScale + 0.5).toInt(),
            DungeonMapRenderData(
                rooms, doors,
                DungeonMapRenderOptions(
                    mapOf(
                        DungeonMapColors.RoomEntrance to SETTING_ROOM_ENTRANCE_COLOR.getColor(),
                        DungeonMapColors.RoomNormal to SETTING_ROOM_NORMAL_COLOR.getColor(),
                        DungeonMapColors.RoomMiniboss to SETTING_ROOM_MINIBOSS_COLOR.getColor(),
                        DungeonMapColors.RoomFairy to SETTING_ROOM_FAIRY_COLOR.getColor(),
                        DungeonMapColors.RoomBlood to SETTING_ROOM_BLOOD_COLOR.getColor(),
                        DungeonMapColors.RoomPuzzle to SETTING_ROOM_PUZZLE_COLOR.getColor(),
                        DungeonMapColors.RoomTrap to SETTING_ROOM_TRAP_COLOR.getColor(),
                        DungeonMapColors.RoomYellow to SETTING_ROOM_YELLOW_COLOR.getColor(),
                        DungeonMapColors.RoomRare to SETTING_ROOM_RARE_COLOR.getColor(),
                        DungeonMapColors.RoomUnknown to SETTING_ROOM_UNKNOWN_COLOR.getColor(),

                        DungeonMapColors.DoorWither to SETTING_DOOR_WITHER_COLOR.getColor(),
                        DungeonMapColors.DoorBlood to SETTING_DOOR_BLOOD_COLOR.getColor(),
                        DungeonMapColors.DoorEntrance to SETTING_DOOR_ENTRANCE_COLOR.getColor()
                    ),
                    SETTING_ROOM_SIZE.get(), SETTING_DOOR_SIZE.get(),
                    Dungeons.floor.roomsW, Dungeons.floor.roomsH,
                    SETTING_RENDER_CHECKMARK.get(), SETTING_RENDER_PUZZLE_ICON.get(),
                    SETTING_RENDER_ROOM_NAMES.get(),
                    SETTING_RENDER_SECRET_COUNT.get(),
                    SETTING_RENDER_PUZZLE_NAME.get(),
                    SETTING_ICON_SIZE.get(),
                    DungeonMapRoomInfoAlignment.from(SETTING_ICON_ALIGNMENT.getCurrent()),
                    SETTING_TEXT_SIZE.get(),
                    DungeonMapRoomInfoAlignment.from(SETTING_TEXT_ALIGNMENT.getCurrent()),
                    SETTING_TEXT_SHADOW.get(),
                    SETTING_COLOR_ROOM_TEXT.get(),
                    SETTING_RENDER_HIDDEN_ROOMS.get(),
                    Dungeons.started.value,
                    SETTING_HIDDEN_ROOM_DARKEN.get()
                )
            )
        )
    }

    private val textHuds by lazy {
        List(5) {
            SimpleTextHud("dungeon_map_name_$it").also {
                it.x = 0.0
                it.y = -10.0
                it.scale = 1f
                it.anchor = TextHud.Anchor.Center
                it.align = TextHud.Align.Center
                it.backdrop = TextHud.Backdrop.None
            }
        }
    }

    override fun drawImpl(ctx: GuiGraphics) {
        if (Dungeons.inBoss.value) return
        if (Dungeons.floor == FloorType.None) return
        mapRenderer.draw(ctx, x.toFloat(), y.toFloat(), (1.0 / minecraft.window.guiScale).toFloat())

        val bounds = getBounds()

        val renderNames = SETTING_RENDER_NAMES.get() && (if (SETTING_RENDER_NAMES_ONLY_LEAP.get()) {
            val held = minecraft.player?.mainHandItem
            if (held != null) listOf("SPIRIT_LEAP", "INFINITE_SPIRIT_LEAP").contains(ItemUtils.skyblockId(held))
            else false
        } else true)

        // shrugs
        var idx = 0
        Dungeons.players.forEach { (_, player) ->
            val i = idx++
            val pos = player.getLerpedPosition() ?: return@forEach

            val px = (bounds.x + pos.x / (Dungeons.floor.maxDim * 2.0) * bounds.w).toFloat()
            val py = (bounds.y + pos.z / (Dungeons.floor.maxDim * 2.0) * bounds.h).toFloat()

            if (renderNames) {
                val text =
                    (if (SETTING_COLOR_NAME_BY_CLASS.get()) player.role.colorCode else "") +
                    if (SETTING_USE_CLASS_NAME.get() && player.role != DungeonClass.Unknown) player.role.shortName else player.name

                val hud = textHuds[i]
                hud.x = px.toDouble()
                hud.y = py - 5.0
                hud.shadow = SETTING_TEXT_SHADOW.get()
                hud.setLine(text)
                hud.scale = scale * 0.3f * SETTING_NAME_SCALE.get().toFloat()
                hud.draw(ctx)
            }

            val dxf = cos(-pos.r).toFloat() * 2.8f * SETTING_MARKER_SCALE.get().toFloat()
            val dyf = sin(-pos.r).toFloat() * 2.8f * SETTING_MARKER_SCALE.get().toFloat()
            val dxr = cos(-pos.r + PI / 2).toFloat() * 2f * SETTING_MARKER_SCALE.get().toFloat()
            val dyr = sin(-pos.r + PI / 2).toFloat() * 2f * SETTING_MARKER_SCALE.get().toFloat()

            val textureView = (if (i == 0) markerSelfUploader else markerOtherUploader)?.textureView ?: return@forEach
            ctx.guiRenderState.submitGuiElement(
                TexturedQuadRenderState(
                    BufferedImageRenderer.pipeline,
                    TextureSetup.singleTexture(textureView),
                    Matrix3x2f(ctx.pose()),
                    px + dxf - dxr, py + dyf - dyr,
                    px - dxf - dxr, py - dyf - dyr,
                    px + dxf + dxr, py + dyf + dyr,
                    px - dxf + dxr, py - dyf + dyr,
                    0f, 0f,
                    0f, 1f,
                    1f, 0f,
                    1f, 1f,
                    -1,
                    ctx.scissorStack.peek()
                )
            )
        }
    }

    override fun initialize() {
        on<RenderOverlayEvent> { event ->
            draw(event.ctx)
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        mapRenderer.invalidate()
    }

    val mcidMarkerSelf = ResourceLocation.fromNamespaceAndPath("devonian", "dungeon_map_marker_self")
    val mcidMarkerOther = ResourceLocation.fromNamespaceAndPath("devonian", "dungeon_map_marker_other")
    val markerSelfUploader = BufferedImageUploader.fromResource("/assets/devonian/dungeons/map/markerSelf.png")
        ?.register(mcidMarkerSelf)
    val markerOtherUploader = BufferedImageUploader.fromResource("/assets/devonian/dungeons/map/markerOther.png")
        ?.register(mcidMarkerOther)
}