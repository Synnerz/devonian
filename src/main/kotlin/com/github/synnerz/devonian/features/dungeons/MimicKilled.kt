package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.events.EntityDeathEvent
import com.github.synnerz.devonian.events.WorldChangeEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.ChatUtils
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.mob.ZombieEntity

object MimicKilled : Feature("mimicKilled", "catacombs") {
    private var messageSent = false

    override fun initialize() {
        on<EntityDeathEvent> { event ->
            if (messageSent) return@on
            val entity = event.entity as LivingEntity
            if (entity !is ZombieEntity) return@on
            if (!entity.isBaby || entity.hasStackEquipped(EquipmentSlot.HEAD)) return@on

            ChatUtils.command("pc Mimic Killed!")
            messageSent = true
        }

        on<WorldChangeEvent> {
            messageSent = false
        }
    }
}