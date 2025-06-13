package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.events.Events
import com.github.synnerz.devonian.features.Feature
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexRendering
import net.minecraft.entity.decoration.ArmorStandEntity

object BoxStarMob : Feature() {
    val starMobEntities = mutableListOf<Int>()

    override fun initialize() {
        Events.onEntityAdd { entity, _ ->
            if (entity !is ArmorStandEntity) return@onEntityAdd

            Events.scheduleStandName(entity, {
                val entityName = entity.name.string
                val entityId = entity.id
                if (!entityName.contains("✯ ")) return@scheduleStandName

                val nextId = if (entityName.contains("Withermancer")) 3 else 1
                val ents = minecraft.world?.getEntityById(entityId - nextId) ?: return@scheduleStandName

                starMobEntities.add(ents.id)
            })
        }

        Events.onWorldChange { _, _ ->
            starMobEntities.clear()
        }

        Events.onPreRenderEntity { entity, matrixStack, vertexConsumerProvider, _, _ ->
            if (!starMobEntities.contains(entity.id)) return@onPreRenderEntity

            val cam = minecraft.gameRenderer.camera.pos.negate()
            val width = entity.width + 0.2
            val height = entity.height
            val halfWidth = width / 2

            matrixStack.push()
            matrixStack.translate(cam.x, cam.y, cam.z)

            VertexRendering.drawBox(
                matrixStack,
                vertexConsumerProvider.getBuffer(RenderLayer.getLines()),
                entity.x - halfWidth, entity.y, entity.z - halfWidth,
                entity.x + halfWidth, entity.y + height, entity.z + halfWidth,
                0f, 1f, 1f, 1f
            )
            matrixStack.pop()
        }
    }
}