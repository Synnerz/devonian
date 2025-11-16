package com.github.synnerz.devonian.features.dungeons.map

import com.github.synnerz.devonian.api.dungeon.DungeonDoor
import com.github.synnerz.devonian.api.dungeon.DungeonRoom
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

    // TODO: hey doc remember to clone the list before passing it in so you dont get a conc mod :(
    fun redrawMap(rooms: List<DungeonRoom?>, doors: List<DungeonDoor?>) {
        val bounds = getBounds()
        mapRenderer.update(
            (bounds.w + 0.5).toInt(),
            (bounds.h + 0.5).toInt(),
            DungeonMapRenderData(
                rooms, doors,
                DungeonMapRenderOptions(
                    mapOf(
                        DungeonMapColors.RoomEntrance to Color(20, 133, 0, 255),
                        DungeonMapColors.RoomNormal to Color(107, 58, 17, 255),
                        DungeonMapColors.RoomMiniboss to Color(254, 223, 0, 255),
                        DungeonMapColors.RoomFairy to Color(224, 0, 255, 255),
                        DungeonMapColors.RoomBlood to Color(255, 0, 0, 255),
                        DungeonMapColors.RoomPuzzle to Color(117, 0, 133, 255),
                        DungeonMapColors.RoomTrap to Color(216, 127, 51, 255),
                        DungeonMapColors.RoomYellow to Color(254, 223, 0, 255),
                        DungeonMapColors.RoomRare to Color(255, 203, 89, 255),
                        DungeonMapColors.RoomUnknown to Color(255, 176, 31, 255),
                        DungeonMapColors.DoorWither to Color.BLACK,
                        DungeonMapColors.DoorBlood to Color.RED,
                        DungeonMapColors.DoorEntrance to Color.GREEN
                    ),
                    0.9, 0.6, 6,
                    true, true, true,
                    DungeonMapRoomInfoAlignment.TopLeft,
                    true, true, true,
                    1.0, true, true, true
                )
            )
        )
    }

    override fun drawImpl(ctx: DrawContext) {
        mapRenderer.draw(ctx, x.toFloat(), y.toFloat(), scale)

        // TODO: draw players
    }
}