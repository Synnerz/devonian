package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.events.EntityDeathEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.features.Feature
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.monster.Zombie

object MimicKilled : Feature(
    "mimicKilled",
    "Whenever a mimic is killed it will send a party message.",
    "Dungeons",
    "catacombs"
) {
    private var messageSent = false

    override fun initialize() {
        on<EntityDeathEvent> { event ->
            if (messageSent) return@on
            val entity = event.entity as LivingEntity
            if (entity !is Zombie) return@on
            if (!entity.isBaby || entity.hasItemInSlot(EquipmentSlot.HEAD)) return@on

            ChatUtils.command("pc Mimic Killed!")
            messageSent = true
            Dungeons.mimicKilled.value = true
        }

        on<WorldChangeEvent> {
            messageSent = false
        }
    }
}