package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.events.*
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.Scheduler
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexRendering
import net.minecraft.entity.decoration.ArmorStandEntity

object BoxStarMob : Feature("boxStarMob", "catacombs") {
    val starMobEntities = mutableListOf<Int>()

    override fun initialize() {
        on<EntityJoinEvent> { event ->
            val entity = event.entity
            if (entity !is ArmorStandEntity) return@on

            Scheduler.scheduleStandName(entity, {
                val entityName = entity.name.string
                val entityId = entity.id
                if (!entityName.contains("✯ ")) return@scheduleStandName

                val previousId = if (entityName.contains("Withermancer")) 3 else 1
                val entityBelow = minecraft.world?.getEntityById(entityId - previousId) ?: return@scheduleStandName

                starMobEntities.add(entityBelow.id)
            })
        }

        on<WorldChangeEvent> {
            starMobEntities.clear()
        }

        on<RenderEntityEvent> { event ->
            val entity = event.entity
            val matrixStack = event.matrixStack
            val consumer = event.consumer
            if (!starMobEntities.contains(entity.id)) return@on

            val cam = minecraft.gameRenderer.camera.pos.negate()
            val width = entity.width + 0.2
            val height = entity.height
            val halfWidth = width / 2

            matrixStack.push()
            matrixStack.translate(cam.x, cam.y, cam.z)

            VertexRendering.drawBox(
                matrixStack,
                consumer.getBuffer(RenderLayer.getLines()),
                entity.x - halfWidth, entity.y, entity.z - halfWidth,
                entity.x + halfWidth, entity.y + height, entity.z + halfWidth,
                0f, 1f, 1f, 1f
            )
            matrixStack.pop()
        }
    }
}