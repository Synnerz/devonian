package com.github.synnerz.devonian.features.dungeons.clear

import com.github.synnerz.devonian.api.WorldUtils
import com.github.synnerz.devonian.api.dungeon.DungeonScanner
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.api.events.TickEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.render.Render3DImmediate
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import java.awt.Color

object BoxMimicChest : Feature(
    "boxMimicChest",
    "Draws a box around mimic chest.",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "Highlights",
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.Clear.isActiveState)
    }

    private val SETTING_COLOR = addColorPicker(
        "color",
        Color(209, 29, 5, 160).rgb,
        "",
        "Mimic Chest Color",
    )

    private val chests = mutableListOf<BlockPos>()

    override fun initialize() {
        on<TickEvent> {
            chests.clear()

            val room = DungeonScanner.currentRoom ?: return@on
            if (!room.hasRotation()) return@on

            val id = room.roomID ?: return@on
            val waypoints = DungeonWaypoints.waypointsData.getOrNull(id)?.waypoints ?: return@on
            val chestLocs = waypoints[DungeonWaypoints.WaypointType.CHEST] ?: return@on

            chestLocs.forEach { (cx, y, cz) ->
                val (x, z) = room.fromComp(cx, cz) ?: return@forEach
                val bs = WorldUtils.getBlockState(x, y, z) ?: return@forEach
                if (bs.block === Blocks.TRAPPED_CHEST) chests.add(BlockPos(x, y, z))
            }
        }

        on<RenderWorldEvent> {
            chests.forEach {
                Render3DImmediate.renderFilledBox(
                    it.x + 0.05, it.y + 0.0, it.z + 0.05,
                    0.9, 0.9,
                    SETTING_COLOR.getColor(),
                    phase = false,
                )
            }
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        chests.clear()
    }
}