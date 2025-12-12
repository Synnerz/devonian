package com.github.synnerz.devonian.features.dungeons.map

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.bufimgrenderer.BufferedImageRenderer
import com.github.synnerz.devonian.api.bufimgrenderer.BufferedImageUploader
import com.github.synnerz.devonian.api.dungeon.*
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.HudFeature
import com.github.synnerz.devonian.hud.texthud.StylizedTextHud
import com.github.synnerz.devonian.hud.texthud.StylizedTextHud.*
import com.github.synnerz.devonian.utils.BoundingBox
import com.github.synnerz.devonian.utils.TexturedQuadRenderState
import com.github.synnerz.devonian.utils.math.MathUtils
import com.mojang.blaze3d.textures.GpuTextureView
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.PlayerFaceRenderer.*
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
    Categories.DUNGEON_MAP,
    "catacombs",
    subcategory = "Toggle",
) {
    private val SETTING_USE_PLAYER_HEADS = addSwitch(
        "playerHeads",
        false,
        "",
        "Render Player Heads",
        subcategory = "Markers",
    )
    private val SETTING_USE_MARKER_SELF = addSwitch(
        "markerSelf",
        true,
        "",
        "Use Marker for Self",
        subcategory = "Markers",
    )
    private val SETTING_RENDER_NAMES = addSwitch(
        "renderNames",
        true,
        "Render player names above marker",
        "Render Names",
        subcategory = "Markers",
    )
    private val SETTING_RENDER_NAMES_ONLY_LEAP = addSwitch(
        "namesRequireLeap",
        false,
        "Only render player names when holding leap",
        "Render Names When Holding Leap",
        subcategory = "Markers",
    )
    private val SETTING_USE_CLASS_NAME = addSwitch(
        "useClassName",
        true,
        "Render the class name instead of the player name",
        "Render Class Name",
        subcategory = "Markers",
    )
    private val SETTING_COLOR_NAME_BY_CLASS = addSwitch(
        "colorNameClass",
        true,
        "Colors the player names by their respective class",
        "Color Player Names",
        subcategory = "Markers",
    )
    private val SETTING_COLOR_MARKER_BY_CLASS = addSwitch(
        "colorMarkerClass",
        true,
        "Colors the player marker by their respective class",
        "Color Player Markers",
        subcategory = "Markers",
    )
    private val SETTING_NAME_SCALE = addSlider(
        "nameScale",
        1.0,
        0.0, 10.0,
        "",
        "Player Name Scale",
        subcategory = "Markers",
    )
    private val SETTING_MARKER_SCALE = addSlider(
        "markerScale",
        1.0,
        0.0, 10.0,
        "",
        "Marker Scale",
        subcategory = "Markers",
    )
    private val SETTING_MAP_BACKGROUND_COLOR = addColorPicker(
        "backgroundColor",
        0,
        "",
        "Map Background Color",
        subcategory = "Colors",
    )
    private val SETTING_MAP_PADDING = addDecimalSlider(
        "padding",
        0.0,
        0.0, 2.0,
        "measured in room widths",
        "Map Padding",
        subcategory = "Style",
    )
    private val SETTING_ROOM_ENTRANCE_COLOR = addColorPicker(
        "roomEntranceColor",
        Color(0, 123, 0).rgb,
        "",
        "Entrance Room Color",
        subcategory = "Colors",
    )
    private val SETTING_ROOM_NORMAL_COLOR = addColorPicker(
        "roomNormalColor",
        Color(114, 67, 27).rgb,
        "",
        "Normal Room Color",
        subcategory = "Colors",
    )
    private val SETTING_ROOM_MINIBOSS_COLOR = addColorPicker(
        "roomMinibossColor",
        Color(114, 67, 27).rgb,
        "(as in: has a miniboss, not yellow)",
        "Miniboss Room Color",
        subcategory = "Colors",
    )
    private val SETTING_ROOM_FAIRY_COLOR = addColorPicker(
        "roomFairyColor",
        Color(239, 126, 163).rgb,
        "",
        "Fairy Room Color",
        subcategory = "Colors",
    )
    private val SETTING_ROOM_BLOOD_COLOR = addColorPicker(
        "roomBloodColor",
        Color(255, 0, 0).rgb,
        "",
        "Blood Room Color",
        subcategory = "Colors",
    )
    private val SETTING_ROOM_PUZZLE_COLOR = addColorPicker(
        "roomPuzzleColor",
        Color(176, 75, 213).rgb,
        "",
        "Puzzle Room Color",
        subcategory = "Colors",
    )
    private val SETTING_ROOM_TRAP_COLOR = addColorPicker(
        "roomTrapColor",
        Color(213, 126, 50).rgb,
        "",
        "Trap Room Color",
        subcategory = "Colors",
    )
    private val SETTING_ROOM_YELLOW_COLOR = addColorPicker(
        "roomYellowColor",
        Color(226, 226, 50).rgb,
        "",
        "Yellow Room Color",
        subcategory = "Colors",
    )
    private val SETTING_ROOM_RARE_COLOR = addColorPicker(
        "roomRareColor",
        Color(0, 67, 27).rgb,
        "",
        "Rare Room Color",
        subcategory = "Colors",
    )
    private val SETTING_ROOM_UNKNOWN_COLOR = addColorPicker(
        "roomUnknownColor",
        Color(64, 64, 64).rgb,
        "",
        "Unknown Room Color",
        subcategory = "Colors",
    )
    private val SETTING_DOOR_WITHER_COLOR = addColorPicker(
        "doorWitherColor",
        Color(0, 0, 0).rgb,
        "",
        "Wither Door Color",
        subcategory = "Colors",
    )
    private val SETTING_DOOR_BLOOD_COLOR = addColorPicker(
        "doorBloodColor",
        Color(255, 0, 0).rgb,
        "",
        "Blood Door Color",
        subcategory = "Colors",
    )
    private val SETTING_DOOR_ENTRANCE_COLOR = addColorPicker(
        "doorEntranceColor",
        Color(0, 123, 0).rgb,
        "",
        "Entrance Door Color",
        subcategory = "Colors",
    )
    private val SETTING_ROOM_SIZE = addDecimalSlider(
        "roomSize",
        0.8,
        0.0, 1.0,
        "",
        "Room Size",
        subcategory = "Style",
    )
    private val SETTING_DOOR_SIZE = addDecimalSlider(
        "doorSize",
        0.4,
        0.0, 1.0,
        "",
        "Door Size",
        subcategory = "Style",
    )
    private val SETTING_RENDER_CHECKMARK = addSwitch(
        "renderCheckmark",
        true,
        "",
        "Render Checkmarks",
        subcategory = "Behavior",
    )
    private val SETTING_RENDER_PUZZLE_ICON = addSwitch(
        "renderPuzzleIcon",
        true,
        "",
        "Render Puzzle Icon",
        subcategory = "Behavior",
    )
    private val SETTING_RENDER_ROOM_NAMES = addSwitch(
        "renderRoomNames",
        true,
        "",
        "Render Room Names",
        subcategory = "Behavior",
    )
    private val SETTING_RENDER_SECRET_COUNT = addSwitch(
        "renderSecretCount",
        false,
        "(we dont actually track or sync secrets right now)",
        "Render Secret Count",
        subcategory = "Behavior",
    )
    private val SETTING_RENDER_PUZZLE_NAME = addSwitch(
        "renderPuzzleName",
        false,
        "",
        "Render Puzzle Name",
        subcategory = "Behavior",
    )
    private val SETTING_ICON_SIZE = addDecimalSlider(
        "iconSize",
        0.6,
        0.0, 2.0,
        "Affects puzzles + checkmarks. (% of the room)",
        "Icon Size",
        subcategory = "Style",
    )
    private val SETTING_ICON_ALIGNMENT = addSelection(
        "iconAlign",
        DungeonMapRoomInfoAlignment.Center.ordinal,
        DungeonMapRoomInfoAlignment.entries.map { it.str },
        "Alignment of the icon with respect to the room layout",
        "Icon Alignment",
        subcategory = "Style",
    )
    private val SETTING_TEXT_SIZE = addDecimalSlider(
        "textSize",
        0.8,
        0.0, 2.0,
        "Affects room names + secret count. (% of the room)",
        "Text Size",
        subcategory = "Style",
    )
    private val SETTING_TEXT_ALIGNMENT = addSelection(
        "textAlign",
        DungeonMapRoomInfoAlignment.TopLeft.ordinal,
        DungeonMapRoomInfoAlignment.entries.map { it.str },
        "Alignment of the text with respect to the room layout",
        "Text Alignment",
        subcategory = "Style",
    )
    private val SETTING_TEXT_SHADOW = addSwitch(
        "textShadow",
        true,
        "",
        "Text Shadow",
        subcategory = "Style",
    )
    private val SETTING_COLOR_ROOM_TEXT = addSwitch(
        "colorRoomName",
        true,
        "Change color of room name based on the room checkmark",
        "Color Room Name",
        subcategory = "Style",
    )
    private val SETTING_RENDER_HIDDEN_ROOMS = addSwitch(
        "renderHiddenRooms",
        false,
        "",
        "Render Hidden Rooms",
        subcategory = "Behavior",
        cheeto = true,
        isHidden = true,
    )
    private val SETTING_HIDDEN_ROOM_DARKEN = addDecimalSlider(
        "hiddenRoomDarken",
        0.7,
        0.0, 1.0,
        "factor by which to darken hidden rooms",
        "Hidden Room Darken Factor",
        subcategory = "Style",
    )

    private val mapRenderer = DungeonMapBaseRenderer()

    private var dump = false

    init {
        DevonianCommand.command.subcommand("dumpmap") { _, _ ->
            dump = true
            return@subcommand 1
        }
    }

    override fun getBounds(): BoundingBox = BoundingBox(
        x, y,
        100.0 * scale, 100.0 * scale
    )

    fun redrawMap(rooms: List<DungeonRoom?>, doors: List<DungeonDoor?>) {
        var floor = Dungeons.floor
        if (floor == FloorType.None) floor = FloorType.M7

        if (dump) {
            println("Rooms:")
            rooms.forEach {
                if (it == null) return@forEach
                println("${System.identityHashCode(it)} ${it.name} ${it.explored} ${it.type.name} ${it.rotation} ${it.comps.joinToString(",") { it.toComponent().toString() }} ${it.doors.joinToString(",") { "${System.identityHashCode(it)} ${it.type.name} ${it.opened} ${it.comp.toComponent()}" }}")
            }
            println("Doors:")
            doors.forEach {
                if (it == null) return@forEach
                println("${System.identityHashCode(it)} ${it.type.name} ${it.opened} ${it.comp.toComponent()} ${it.rooms.joinToString(",") { "${System.identityHashCode(it)} ${it.name} ${it.explored} ${it.comps.joinToString(",") { it.toComponent().toString() }}" }}")
            }
            dump = false
        }

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
                    floor.roomsW, floor.roomsH,
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
            StylizedTextHud("dungeon_map_name_$it").also {
                it.x = 0.0
                it.y = -10.0
                it.scale = 1f
                it.anchor = Anchor.Center
                it.align = Align.Center
                it.backdrop = Backdrop.None
            }
        }
    }

    override fun drawImpl(ctx: GuiGraphics) {
        if (Dungeons.inBoss.value) return
        var floor = Dungeons.floor
        if (floor == FloorType.None) floor = FloorType.M7
        mapRenderer.draw(ctx, x.toFloat(), y.toFloat(), (1.0 / minecraft.window.guiScale).toFloat())

        val bounds = getBounds()

        val totalMaxDim = floor.maxDim + SETTING_MAP_PADDING.get() * 2
        val boundsOX = (floor.maxDim - floor.roomsW) / 2.0 + SETTING_MAP_PADDING.get()
        val boundsOY = (floor.maxDim - floor.roomsH) / 2.0 + SETTING_MAP_PADDING.get()
        val compBounds = BoundingBox(
            bounds.x + boundsOX / totalMaxDim * bounds.w,
            bounds.y + boundsOY / totalMaxDim * bounds.h,
            floor.maxDim / totalMaxDim * bounds.w,
            floor.maxDim / totalMaxDim * bounds.h
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
            if (player.isDead) return@forEach
            val pos = player.getLerpedPosition() ?: return@forEach

            val px = MathUtils.rescale(
                pos.x,
                0.0, floor.maxDim * 2.0,
                compBounds.x, compBounds.x + compBounds.w
            ).toFloat()
            val py = MathUtils.rescale(
                pos.z,
                0.0, floor.maxDim * 2.0,
                compBounds.y, compBounds.y + compBounds.h
            ).toFloat()

            var dxf = cos(-pos.r).toFloat() * SETTING_MARKER_SCALE.get().toFloat()
            var dyf = sin(-pos.r).toFloat() * SETTING_MARKER_SCALE.get().toFloat()
            var dxr = cos(-pos.r + PI / 2).toFloat() * SETTING_MARKER_SCALE.get().toFloat()
            var dyr = sin(-pos.r + PI / 2).toFloat() * SETTING_MARKER_SCALE.get().toFloat()
            val u0: Float
            val v0: Float
            val u1: Float
            val v1: Float
            val maxDy: Float
            val textureView: GpuTextureView
            val info = player.profileInfo
            val isHead =
                SETTING_USE_PLAYER_HEADS.get() && info != null &&
                (!SETTING_USE_MARKER_SELF.get() || i > 0)
            if (isHead) {
                dxf *= 4f
                dyf *= 4f
                dxr *= 4f
                dyr *= 4f
                u0 = SKIN_HEAD_U.toFloat() / SKIN_TEX_WIDTH
                v0 = SKIN_HEAD_V.toFloat() / SKIN_TEX_HEIGHT
                u1 = (SKIN_HEAD_U + SKIN_HEAD_WIDTH).toFloat() / SKIN_TEX_WIDTH
                v1 = (SKIN_HEAD_V + SKIN_HEAD_HEIGHT).toFloat() / SKIN_TEX_HEIGHT
                maxDy = 4f
                val skin = info.skin
                val rl = skin.body.texturePath()
                textureView = Devonian.minecraft.textureManager.getTexture(rl).textureView
            } else {
                dxf *= 2.8f
                dyf *= 2.8f
                dxr *= 2f
                dyr *= 2f
                u0 = if (i == 0) MARKER_SELF_U0 else MARKER_OTHER_U0
                v0 = if (i == 0) MARKER_SELF_V0 else MARKER_OTHER_V0
                u1 = if (i == 0) MARKER_SELF_U1 else MARKER_OTHER_U1
                v1 = if (i == 0) MARKER_SELF_V1 else MARKER_OTHER_V1
                maxDy = 2.8f
                textureView = markerAtlasUploader.textureView
            }

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
                hud.y = py - maxDy * SETTING_MARKER_SCALE.get().toFloat() - hud.getHeight() * 0.5
                hud.shadow = SETTING_TEXT_SHADOW.get()
                hud.setLine("$nameFormat$text")
                hud.scale = scale * 0.3f * SETTING_NAME_SCALE.get().toFloat()
                hud.draw(ctx)
            }

            ctx.guiRenderState.submitGuiElement(
                TexturedQuadRenderState(
                    BufferedImageRenderer.pipeline,
                    TextureSetup.singleTexture(textureView),
                    Matrix3x2f(ctx.pose()),
                    px + dxf - dxr, py + dyf - dyr,
                    px - dxf - dxr, py - dyf - dyr,
                    px + dxf + dxr, py + dyf + dyr,
                    px - dxf + dxr, py - dyf + dyr,
                    u0, v0,
                    u0, v1,
                    u1, v0,
                    u1, v1,
                    -1,
                    ctx.scissorStack.peek()
                )
            )

            if (SETTING_COLOR_MARKER_BY_CLASS.get() && player.role.color.alpha != 0) {
                val u0: Float
                val v0: Float
                val u1: Float
                val v1: Float
                if (isHead) {
                    u0 = MARKER_HEAD_OUTLINE_U0
                    v0 = MARKER_HEAD_OUTLINE_V0
                    u1 = MARKER_HEAD_OUTLINE_U1
                    v1 = MARKER_HEAD_OUTLINE_V1
                } else {
                    u0 = MARKER_POINTER_OUTLINE_U0
                    v0 = MARKER_POINTER_OUTLINE_V0
                    u1 = MARKER_POINTER_OUTLINE_U1
                    v1 = MARKER_POINTER_OUTLINE_V1
                }

                ctx.guiRenderState.submitGuiElement(
                    TexturedQuadRenderState(
                        BufferedImageRenderer.pipeline,
                        TextureSetup.singleTexture(markerAtlasUploader.textureView),
                        Matrix3x2f(ctx.pose()),
                        px + dxf - dxr, py + dyf - dyr,
                        px - dxf - dxr, py - dyf - dyr,
                        px + dxf + dxr, py + dyf + dyr,
                        px - dxf + dxr, py - dyf + dyr,
                        u0, v0,
                        u0, v1,
                        u1, v0,
                        u1, v1,
                        player.role.colorRgb,
                        ctx.scissorStack.peek()
                    )
                )
            }
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

    val mcidMarkerAtlas = ResourceLocation.fromNamespaceAndPath("devonian", "dungeon_map_marker_atlas")!!
    val markerAtlasUploader = BufferedImageUploader.fromResource("/assets/devonian/dungeons/map/markerAtlas.png")!!
        .register(mcidMarkerAtlas)

    val MARKER_SELF_U0: Float
    val MARKER_SELF_V0: Float
    val MARKER_SELF_U1: Float
    val MARKER_SELF_V1: Float

    val MARKER_OTHER_U0: Float
    val MARKER_OTHER_V0: Float
    val MARKER_OTHER_U1: Float
    val MARKER_OTHER_V1: Float

    val MARKER_POINTER_OUTLINE_U0: Float
    val MARKER_POINTER_OUTLINE_V0: Float
    val MARKER_POINTER_OUTLINE_U1: Float
    val MARKER_POINTER_OUTLINE_V1: Float

    val MARKER_HEAD_OUTLINE_U0: Float
    val MARKER_HEAD_OUTLINE_V0: Float
    val MARKER_HEAD_OUTLINE_U1: Float
    val MARKER_HEAD_OUTLINE_V1: Float

    init {
        val MARKER_ATLAS_WIDTH = 200 * 1f
        val MARKER_ATLAS_HEIGHT = 280 * 1f

        val MARKER_WIDTH = 100
        val MARKER_HEIGHT = 140
        val selfX = 0
        val selfY = 0
        val otherX = MARKER_WIDTH
        val otherY = 0

        val pointerOutlineX = 0
        val pointerOutlineY = MARKER_HEIGHT

        val HEAD_WIDTH = 80
        val HEAD_HEIGHT = 80
        val headOutlineX = MARKER_WIDTH
        val headOutlineY = MARKER_HEIGHT

        MARKER_SELF_U0 = selfX / MARKER_ATLAS_WIDTH
        MARKER_SELF_V0 = selfY / MARKER_ATLAS_HEIGHT
        MARKER_SELF_U1 = (selfX + MARKER_WIDTH) / MARKER_ATLAS_WIDTH
        MARKER_SELF_V1 = (selfY + MARKER_HEIGHT) / MARKER_ATLAS_HEIGHT

        MARKER_OTHER_U0 = otherX / MARKER_ATLAS_WIDTH
        MARKER_OTHER_V0 = otherY / MARKER_ATLAS_HEIGHT
        MARKER_OTHER_U1 = (otherX + MARKER_WIDTH) / MARKER_ATLAS_WIDTH
        MARKER_OTHER_V1 = (otherY + MARKER_HEIGHT) / MARKER_ATLAS_HEIGHT

        MARKER_POINTER_OUTLINE_U0 = pointerOutlineX / MARKER_ATLAS_WIDTH
        MARKER_POINTER_OUTLINE_V0 = pointerOutlineY / MARKER_ATLAS_HEIGHT
        MARKER_POINTER_OUTLINE_U1 = (pointerOutlineX + MARKER_WIDTH) / MARKER_ATLAS_WIDTH
        MARKER_POINTER_OUTLINE_V1 = (pointerOutlineY + MARKER_HEIGHT) / MARKER_ATLAS_HEIGHT

        MARKER_HEAD_OUTLINE_U0 = headOutlineX / MARKER_ATLAS_WIDTH
        MARKER_HEAD_OUTLINE_V0 = headOutlineY / MARKER_ATLAS_HEIGHT
        MARKER_HEAD_OUTLINE_U1 = (headOutlineX + HEAD_WIDTH) / MARKER_ATLAS_WIDTH
        MARKER_HEAD_OUTLINE_V1 = (headOutlineY + HEAD_HEIGHT) / MARKER_ATLAS_HEIGHT
    }
}