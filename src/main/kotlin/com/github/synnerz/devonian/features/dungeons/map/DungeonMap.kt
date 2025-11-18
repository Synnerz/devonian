package com.github.synnerz.devonian.features.dungeons.map

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.bufimgrenderer.BufferedImageRenderer.Companion.pipeline
import com.github.synnerz.devonian.api.bufimgrenderer.BufferedImageUploader
import com.github.synnerz.devonian.api.dungeon.DungeonClass
import com.github.synnerz.devonian.api.dungeon.DungeonDoor
import com.github.synnerz.devonian.api.dungeon.DungeonRoom
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.hud.HudFeature
import com.github.synnerz.devonian.hud.texthud.SimpleTextHud
import com.github.synnerz.devonian.hud.texthud.TextHud
import com.github.synnerz.devonian.mixin.accessor.DrawContextAccessor
import com.github.synnerz.devonian.utils.BoundingBox
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.RenderPhase
import net.minecraft.util.Identifier
import net.minecraft.util.TriState
import java.awt.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object DungeonMap : HudFeature(
    "dungeonMap",
    "Dungeon Map",
    "Dungeons",
    "catacombs"
) {
    private const val SETTING_RENDER_NAMES = true
    private const val SETTING_RENDER_NAMES_ONLY_LEAP = false
    private const val SETTING_USE_CLASS_NAME = true
    private const val SETTING_COLOR_NAME_BY_CLASS = true
    private const val SETTING_NAME_SCALE = 1f
    private const val SETTING_MARKER_SCALE = 1f

    private val mapRenderer = DungeonMapBaseRenderer()

    override fun getBounds(): BoundingBox = BoundingBox(
        x, y,
        100.0 * scale, 100.0 * scale
    )

    fun redrawMap(rooms: List<DungeonRoom?>, doors: List<DungeonDoor?>) {
        val bounds = getBounds()
        val window = minecraft.window
        mapRenderer.update(
            (bounds.w * window.scaleFactor + 0.5).toInt(),
            (bounds.h * window.scaleFactor + 0.5).toInt(),
            DungeonMapRenderData(
                rooms, doors,
                DungeonMapRenderOptions(
                    mapOf(
                        DungeonMapColors.RoomEntrance to Color(0, 123, 0, 255),
                        DungeonMapColors.RoomNormal to Color(114, 67, 27, 255),
                        DungeonMapColors.RoomMiniboss to Color(57, 67, 27, 255),
                        DungeonMapColors.RoomFairy to Color(239, 126, 163, 255),
                        DungeonMapColors.RoomBlood to Color(255, 0, 0, 255),
                        DungeonMapColors.RoomPuzzle to Color(176, 75, 213, 255),
                        DungeonMapColors.RoomTrap to Color(213, 126, 50, 255),
                        DungeonMapColors.RoomYellow to Color(226, 226, 50, 255),
                        DungeonMapColors.RoomRare to Color(0, 67, 27, 255),
                        DungeonMapColors.RoomUnknown to Color(64, 64, 64, 255),

                        DungeonMapColors.DoorWither to Color(0, 0, 0, 255),
                        DungeonMapColors.DoorBlood to Color(255, 0, 0, 255),
                        DungeonMapColors.DoorEntrance to Color(0, 123, 0, 255)
                    ),
                    0.8, 0.4, 6,
                    true, true, true,
                    0.8, DungeonMapRoomInfoAlignment.TopLeft,
                    0.6, DungeonMapRoomInfoAlignment.Center,
                    true, true, true,
                    0.7, true, false, true
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
                it.anchor = TextHud.Anchor.SW
                it.align = TextHud.Align.CenterIgnoreAnchor
                it.shadow = true
                it.backdrop = TextHud.Backdrop.None
            }
        }
    }

    override fun drawImpl(ctx: DrawContext) {
        mapRenderer.draw(ctx, x.toFloat(), y.toFloat(), (1.0 / minecraft.window.scaleFactor).toFloat())

        val bounds = getBounds()

        val renderNames = SETTING_RENDER_NAMES && (if (SETTING_RENDER_NAMES_ONLY_LEAP) {
            val held = minecraft.player?.activeItem
            if (held != null) listOf("SPIRIT_LEAP", "INFINITE_SPIRIT_LEAP").contains(ItemUtils.skyblockId(held))
            else false
        } else true)

        // shrugs
        var idx = 0
        Dungeons.players.forEach { (_, player) ->
            val i = idx++
            val pos = player.getLerpedPosition() ?: return@forEach

            val px = (bounds.x + pos.x / 12.0 * bounds.w).toFloat()
            val py = (bounds.y + pos.z / 12.0 * bounds.h).toFloat()

            if (renderNames) {
                val text =
                    (if (SETTING_COLOR_NAME_BY_CLASS && player.role != DungeonClass.Unknown) player.role.colorCode else "") +
                    if (SETTING_USE_CLASS_NAME) player.role.shortName else player.name

                val hud = textHuds[i]
                hud.x = px.toDouble()
                hud.y = py - 5.0
                hud.setLine(text)
                hud.scale = scale * 0.3f * SETTING_NAME_SCALE
                hud.draw(ctx)
            }

            val dxf = cos(-pos.r).toFloat() * 2.8f * SETTING_MARKER_SCALE
            val dyf = sin(-pos.r).toFloat() * 2.8f * SETTING_MARKER_SCALE
            val dxr = cos(-pos.r + PI / 2).toFloat() * 2f * SETTING_MARKER_SCALE
            val dyr = sin(-pos.r + PI / 2).toFloat() * 2f * SETTING_MARKER_SCALE

            val mat = ctx.matrices.peek().positionMatrix
            val consumer = (ctx as DrawContextAccessor).vertexConsumers
            val buf = consumer.getBuffer(if (i == 0) layerSelf else layerOther)
            buf.vertex(mat, px + dxf - dxr, py + dyf - dyr, 0f).texture(0f, 0f).color(-1)
            buf.vertex(mat, px - dxf - dxr, py - dyf - dyr, 0f).texture(0f, 1f).color(-1)
            buf.vertex(mat, px + dxf + dxr, py + dyf + dyr, 0f).texture(1f, 0f).color(-1)
            buf.vertex(mat, px - dxf + dxr, py - dyf + dyr, 0f).texture(1f, 1f).color(-1)
        }
    }

    override fun initialize() {
        on<RenderOverlayEvent> { event ->
            draw(event.ctx)
        }
    }

    val mcidMarkerSelf = Identifier.of("devonian", "dungeon_map_marker_self")
    val layerSelf = RenderLayer.of(
        "devonian/dungeon_map_marker_self",
        1536,
        false,
        true,
        pipeline,
        RenderLayer.MultiPhaseParameters
            .builder()
            .texture(RenderPhase.Texture(mcidMarkerSelf, TriState.TRUE, false))
            .build(false)
    )
    val mcidMarkerOther = Identifier.of("devonian", "dungeon_map_marker_other")
    val layerOther = RenderLayer.of(
        "devonian/dungeon_map_marker_other",
        1536,
        false,
        true,
        pipeline,
        RenderLayer.MultiPhaseParameters
            .builder()
            .texture(RenderPhase.Texture(mcidMarkerOther, TriState.TRUE, false))
            .build(false)
    )

    init {
        BufferedImageUploader.fromResource("/assets/devonian/dungeons/map/markerSelf.png")?.register(mcidMarkerSelf)
        BufferedImageUploader.fromResource("/assets/devonian/dungeons/map/markerOther.png")?.register(mcidMarkerOther)
    }
}