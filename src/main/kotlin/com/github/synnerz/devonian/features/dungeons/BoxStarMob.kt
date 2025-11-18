package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.barrl.Context
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.EntityJoinEvent
import com.github.synnerz.devonian.api.events.RenderEntityEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.features.Feature
import net.minecraft.world.entity.decoration.ArmorStand
import java.awt.Color

object BoxStarMob : Feature(
    "boxStarMob",
    "Renders a box surrounding the star mobs in dungeons to complete a room.",
    "Dungeons",
    "catacombs"
) {
    val boxColor = Color(0f, 1f, 1f, 1f)
    val starMobEntities = mutableListOf<Int>()

    override fun initialize() {
        on<EntityJoinEvent> { event ->
            val entity = event.entity
            if (entity !is ArmorStand) return@on

            Scheduler.scheduleStandName(entity, {
                val entityName = entity.name.string
                val entityId = entity.id
                if (!entityName.contains("✯ ")) return@scheduleStandName

                val previousId = if (entityName.contains("Withermancer")) 3 else 1
                val entityBelow = minecraft.level?.getEntity(entityId - previousId) ?: return@scheduleStandName

                starMobEntities.add(entityBelow.id)
            })
        }

        on<WorldChangeEvent> {
            starMobEntities.clear()
        }

        on<RenderEntityEvent> { event ->
            val entity = event.entity
            val matrixStack = event.matrixStack
            if (!starMobEntities.contains(entity.id)) return@on

            val cam = minecraft.gameRenderer.mainCamera.position.reverse()
            val width = entity.bbWidth.toDouble()
            val halfWidth = width / 2.0

            matrixStack.pushPose()
            matrixStack.translate(cam.x, cam.y, cam.z)

            Context.Immediate?.renderBox(
                entity.x - halfWidth, entity.y, entity.z - halfWidth,
                width, entity.bbHeight.toDouble(),
                boxColor, translate = false
            )

            matrixStack.popPose()
        }
    }
}