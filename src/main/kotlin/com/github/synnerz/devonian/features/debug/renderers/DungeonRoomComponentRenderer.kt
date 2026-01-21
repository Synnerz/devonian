package com.github.synnerz.devonian.features.debug.renderers

import com.github.synnerz.devonian.api.WorldUtils
import com.github.synnerz.devonian.api.dungeon.DungeonScanner
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.render.Render3DImmediate
import java.awt.Color

object DungeonRoomComponentRenderer : Feature(
    "dungeonRoomComponentRenderer",
    "",
    Categories.DEBUG,
    "catacombs",
    subcategory = "Renderers",
) {
    override fun initialize() {
        on<RenderWorldEvent> {
            val room = DungeonScanner.currentRoom ?: return@on
            if (!room.hasRotation()) return@on

            val player = minecraft.player ?: return@on
            val pt = minecraft.deltaTracker.getGameTimeDeltaPartialTick(false)
            val posVec = player.getPosition(pt)
            val camVec = player.getEyePosition(pt)
            val px = posVec.x
            val py = camVec.y
            val pz = posVec.z
            val lookVec = player.getViewVector(pt)

            val hitResult = WorldUtils.raycast(
                px, py, pz,
                lookVec.x * 128.0,
                lookVec.y * 128.0,
                lookVec.z * 128.0,
                true,
            ) ?: return@on

            val rotated = room.fromPos(hitResult.x, hitResult.z) ?: return@on

            Render3DImmediate.renderWireframeBox(
                hitResult.x.toDouble(),
                hitResult.y.toDouble(),
                hitResult.z.toDouble(),
                1.0, 1.0,
                Color.RED,
                lineWidth = 3.0,
                true,
            )
            Render3DImmediate.renderString(
                "(${rotated.first}, ${hitResult.y}, ${rotated.second})",
                hitResult.x + 0.5,
                hitResult.y + 1.5,
                hitResult.z + 0.5,
                phase = true,
            )
        }
    }
}