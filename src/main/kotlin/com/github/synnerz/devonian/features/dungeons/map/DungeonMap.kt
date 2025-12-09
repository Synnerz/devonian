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
import com.github.synnerz.devonian.utils.math.MathUtils
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
        true,
        "Render player names above marker",
        "Render Names",
    )
    private val SETTING_RENDER_NAMES_ONLY_LEAP = addSwitch(
        "namesRequireLeap",
        false,
        "Only render player names when holding leap",
        "Render Names When Holding Leap",
    )
    private val SETTING_USE_CLASS_NAME = addSwitch(
        "useClassName",
        true,
        "Render the class name instead of the player name",
        "Render Class Name",
    )
    private val SETTING_COLOR_NAME_BY_CLASS = addSwitch(
        "colorNameClass",
        true,
        "Colors the player names by their respective class",
        "Color Player Names",
    )
    private val SETTING_NAME_SCALE = addSlider(
        "nameScale",
        1.0,
        0.0, 10.0,
        "",
        "Player Name Scale",
    )
    private val SETTING_MARKER_SCALE = addSlider(
        "markerScale",
        1.0,
        0.0, 10.0,
        "",
        "Marker Scale",
    )
    private val SETTING_MAP_BACKGROUND_COLOR = addColorPicker(
        "backgroundColor",
        0,
        "",
        "Map Background Color",
    )
    private val SETTING_MAP_PADDING = addDecimalSlider(
        "padding",
        0.0,
        0.0, 2.0,
        "measured in room widths",
        "Map Padding",
    )
    private val SETTING_ROOM_ENTRANCE_COLOR = addColorPicker(
        "roomEntranceColor",
        Color(0, 123, 0).rgb,
        "",
        "Entrance Room Color",
    )
    private val SETTING_ROOM_NORMAL_COLOR = addColorPicker(
        "roomNormalColor",
        Color(114, 67, 27).rgb,
        "",
        "Normal Room Color",
    )
    private val SETTING_ROOM_MINIBOSS_COLOR = addColorPicker(
        "roomMinibossColor",
        Color(114, 67, 27).rgb,
        "(as in: has a miniboss, not yellow)",
        "Miniboss Room Color",
    )
    private val SETTING_ROOM_FAIRY_COLOR = addColorPicker(
        "roomFairyColor",
        Color(239, 126, 163).rgb,
        "",
        "Fairy Room Color",
    )
    private val SETTING_ROOM_BLOOD_COLOR = addColorPicker(
        "roomBloodColor",
        Color(255, 0, 0).rgb,
        "",
        "Blood Room Color",
    )
    private val SETTING_ROOM_PUZZLE_COLOR = addColorPicker(
        "roomPuzzleColor",
        Color(176, 75, 213).rgb,
        "",
        "Puzzle Room Color",
    )
    private val SETTING_ROOM_TRAP_COLOR = addColorPicker(
        "roomTrapColor",
        Color(213, 126, 50).rgb,
        "",
        "Trap Room Color",
    )
    private val SETTING_ROOM_YELLOW_COLOR = addColorPicker(
        "roomYellowColor",
        Color(226, 226, 50).rgb,
        "",
        "Yellow Room Color",
    )
    private val SETTING_ROOM_RARE_COLOR = addColorPicker(
        "roomRareColor",
        Color(0, 67, 27).rgb,
        "",
        "Rare Room Color",
    )
    private val SETTING_ROOM_UNKNOWN_COLOR = addColorPicker(
        "roomUnknownColor",
        Color(64, 64, 64).rgb,
        "",
        "Unknown Room Color",
    )
    private val SETTING_DOOR_WITHER_COLOR = addColorPicker(
        "doorWitherColor",
        Color(0, 0, 0).rgb,
        "",
        "Wither Door Color",
    )
    private val SETTING_DOOR_BLOOD_COLOR = addColorPicker(
        "doorBloodColor",
        Color(255, 0, 0).rgb,
        "",
        "Blood Door Color",
    )
    private val SETTING_DOOR_ENTRANCE_COLOR = addColorPicker(
        "doorEntranceColor",
        Color(0, 123, 0).rgb,
        "",
        "Entrance Door Color",
    )
    private val SETTING_ROOM_SIZE = addDecimalSlider(
        "roomSize",
        0.8,
        0.0, 1.0,
        "",
        "Room Size",
    )
    private val SETTING_DOOR_SIZE = addDecimalSlider(
        "doorSize",
        0.4,
        0.0, 1.0,
        "",
        "Door Size",
    )
    private val SETTING_RENDER_CHECKMARK = addSwitch(
        "renderCheckmark",
        true,
        "",
        "Render Checkmarks",
    )
    private val SETTING_RENDER_PUZZLE_ICON = addSwitch(
        "renderPuzzleIcon",
        true,
        "",
        "Render Puzzle Icon",
    )
    private val SETTING_RENDER_ROOM_NAMES = addSwitch(
        "renderRoomNames",
        true,
        "",
        "Render Room Names",
    )
    private val SETTING_RENDER_SECRET_COUNT = addSwitch(
        "renderSecretCount",
        false,
        "(we dont actually track or sync secrets right now)",
        "Render Secret Count",
    )
    private val SETTING_RENDER_PUZZLE_NAME = addSwitch(
        "renderPuzzleName",
        false,
        "",
        "Render Puzzle Name",
    )
    private val SETTING_ICON_SIZE = addDecimalSlider(
        "iconSize",
        0.6,
        0.0, 1.0,
        "Affects puzzles + checkmarks. (% of the room)",
        "Icon Size",
    )
    private val SETTING_ICON_ALIGNMENT = addSelection(
        "iconAlign",
        DungeonMapRoomInfoAlignment.Center.ordinal,
        DungeonMapRoomInfoAlignment.entries.map { it.str },
        "Alignment of the icon with respect to the room layout",
        "Icon Alignment",
    )
    private val SETTING_TEXT_SIZE = addDecimalSlider(
        "textSize",
        0.8,
        0.0, 1.0,
        "Affects room names + secret count. (% of the room)",
        "Text Size",
    )
    private val SETTING_TEXT_ALIGNMENT = addSelection(
        "textAlign",
        DungeonMapRoomInfoAlignment.TopLeft.ordinal,
        DungeonMapRoomInfoAlignment.entries.map { it.str },
        "Alignment of the text with respect to the room layout",
        "Text Alignment",
    )
    private val SETTING_TEXT_SHADOW = addSwitch(
        "textShadow",
        true,
        "",
        "Text Shadow",
    )
    private val SETTING_COLOR_ROOM_TEXT = addSwitch(
        "colorRoomName",
        true,
        "Change color of room name based on the room checkmark",
        "Color Room Name",
    )
    private val SETTING_RENDER_HIDDEN_ROOMS = addSwitch(
        "renderHiddenRooms",
        false,
        "",
        "Render Hidden Rooms",
        cheeto = true,
    )
    private val SETTING_HIDDEN_ROOM_DARKEN = addDecimalSlider(
        "hiddenRoomDarken",
        0.7,
        0.0, 1.0,
        "factor by which to darken hidden rooms",
        "Hidden Room Darken Factor",
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
                        DungeonMapColors.Background to SETTING_MAP_BACKGROUND_COLOR.getColor(),

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
                    SETTING_MAP_PADDING.get(),
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

        val totalMaxDim = Dungeons.floor.maxDim + SETTING_MAP_PADDING.get() * 2
        val boundsOX = (Dungeons.floor.maxDim - Dungeons.floor.roomsW) / 2.0 + SETTING_MAP_PADDING.get()
        val boundsOY = (Dungeons.floor.maxDim - Dungeons.floor.roomsH) / 2.0 + SETTING_MAP_PADDING.get()
        val compBounds = BoundingBox(
            bounds.x + boundsOX / totalMaxDim * bounds.w,
            bounds.y + boundsOY / totalMaxDim * bounds.h,
            Dungeons.floor.maxDim / totalMaxDim * bounds.w,
            Dungeons.floor.maxDim / totalMaxDim * bounds.h
        )

        val holdingLeap = minecraft.player!!.mainHandItem.let {
            listOf("SPIRIT_LEAP", "INFINITE_SPIRIT_LEAP").contains(ItemUtils.skyblockId(it))
        }

        val shouldRenderName =
            if (SETTING_RENDER_NAMES_ONLY_LEAP.get()) holdingLeap
            else true
        val renderNames = SETTING_RENDER_NAMES.get() && shouldRenderName

        // shrugs
        var idx = 0
        Dungeons.players.forEach { (_, player) ->
            val i = idx++
            val pos = player.getLerpedPosition() ?: return@forEach

            val px = MathUtils.rescale(
                pos.x,
                0.0, Dungeons.floor.maxDim * 2.0,
                compBounds.x, compBounds.x + compBounds.w
            ).toFloat()
            val py = MathUtils.rescale(
                pos.z,
                0.0, Dungeons.floor.maxDim * 2.0,
                compBounds.y, compBounds.y + compBounds.h
            ).toFloat()

            if (renderNames) {
                val nameFormat =
                    if (SETTING_COLOR_NAME_BY_CLASS.get()) player.role.colorCode
                    else ""
                val text =
                // Force display player name if leap is being held regardless of configurations, maybe should be configurable
                    // or perhaps force this to be the `SETTING_RENDER_NAMES_ONLY_LEAP` standard, since people expect this behavior
                    if (holdingLeap) player.name
                    else if (SETTING_USE_CLASS_NAME.get() && player.role != DungeonClass.Unknown) player.role.shortName
                    else player.name

                val hud = textHuds[i]
                hud.x = px.toDouble()
                hud.y = py - 2.8f * SETTING_MARKER_SCALE.get().toFloat() - hud.getHeight() * 0.5
                hud.shadow = SETTING_TEXT_SHADOW.get()
                hud.setLine("$nameFormat$text")
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

    override fun sampleDraw(ctx: GuiGraphics, mx: Int, my: Int, selected: Boolean) {
        val pos = getBounds()
        ctx.drawCenteredString(minecraft.font, "Dungeon Map :)", (pos.x + pos.w / 2.0).toInt(), (pos.y + pos.h / 2.0).toInt(), -1)

        super.sampleDraw(ctx, mx, my, selected)
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