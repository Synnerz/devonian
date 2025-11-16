package com.github.synnerz.devonian.features.dungeons.map

import com.github.synnerz.devonian.api.dungeon.DungeonDoor
import com.github.synnerz.devonian.api.dungeon.DungeonRoom
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.hud.HudFeature
import com.github.synnerz.devonian.utils.BoundingBox
import net.minecraft.client.gui.DrawContext
import java.awt.Color

object DungeonMap : HudFeature(
    "dungeonMap",
    "Dungeon Map",
    "Dungeons",
    "catacombs"
) {
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
                    DungeonMapRoomInfoAlignment.Center,
                    true, true, true,
                    0.7, true, false, true
                )
            )
        )
    }

    override fun drawImpl(ctx: DrawContext) {
        mapRenderer.draw(ctx, x.toFloat(), y.toFloat(), (1.0 / minecraft.window.scaleFactor).toFloat())

        // TODO: draw players
    }

    override fun initialize() {
        on<RenderOverlayEvent> { event ->
            draw(event.ctx)
        }
    }
}