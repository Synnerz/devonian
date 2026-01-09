package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.EntityEquipmentEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import java.util.*

object HideFairy : Feature(
    "hideFairy",
    "",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "Hiders",
) {
    private val fairyUUID = UUID.fromString("93c42dbb-15e2-3d18-8a89-770e440e97d2")

    override fun initialize() {
        on<EntityEquipmentEvent> { event ->
            if (event.type != EntityType.ARMOR_STAND) return@on

            event.slots.forEach { (slot, item) ->
                if (slot != EquipmentSlot.MAINHAND) return@forEach

                val item = item ?: return@on
                if (item.isEmpty) return@on

                val prof = item.get(DataComponents.PROFILE) ?: return@on
                if (prof.partialProfile().id == fairyUUID) Scheduler.scheduleAfterPacket {
                    minecraft.level?.removeEntity(event.entityId, Entity.RemovalReason.DISCARDED)
                }
            }
        }
    }
}