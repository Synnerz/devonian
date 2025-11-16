package com.github.synnerz.devonian.features.dungeons.map

import com.github.synnerz.devonian.api.dungeon.DungeonDoor
import com.github.synnerz.devonian.api.dungeon.DungeonRoom
import com.github.synnerz.devonian.hud.HudFeature
import com.github.synnerz.devonian.utils.BoundingBox
import net.minecraft.client.gui.DrawContext

object DungeonMap : HudFeature("dungeonMap") {
    private val mapRenderer = DungeonMapBaseRenderer()

    override fun getBounds(): BoundingBox = BoundingBox(
        x, y,
        100.0 * scale, 100.0 * scale
    )

    // TODO: hey doc remember to clone the list before passing it in so you dont get a conc mod :(
    fun redrawMap(rooms: List<DungeonRoom>, doors: List<DungeonDoor>) {
        val bounds = getBounds()
        mapRenderer.update(
            (bounds.w + 0.5).toInt(),
            (bounds.h + 0.5).toInt(),
            TODO()
        )
    }

    override fun drawImpl(ctx: DrawContext) {
        mapRenderer.draw(ctx, x.toFloat(), y.toFloat(), scale)

        // TODO: draw players
    }
}