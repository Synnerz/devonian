package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.events.ExtractRenderEntityEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.features.Feature
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.monster.WitherSkeleton

object NoDeathAnimation : Feature(
    "noDeathAnimation",
    "Removes the Death Animation from entities that die."
) {
    private val SETTING_HIDE_DEAD_NAMETAGS = addSwitch(
        "hideTags",
        false,
        "",
        "Hide Dead Nametags",
    )

    private val lividNameRegex = "^\\w+ Livid\$".toRegex()

    private val deadTags = mutableSetOf<Int>()

    private fun shouldHide(entity: LivingEntity): Boolean {
        if (SETTING_HIDE_DEAD_NAMETAGS.get() && entity is ArmorStand && deadTags.contains(entity.id)) return true

        if (entity.isAlive && entity.health > 0f) return false

        val name = entity.name?.string
        if (name != null && lividNameRegex.matches(name)) return false

        val offset = if (entity is WitherSkeleton && (name?.contains("Withermancer") ?: false)) 3 else 1
        deadTags.add(entity.id + offset)

        return true
    }

    override fun initialize() {
        on<ExtractRenderEntityEvent> { event ->
            val entity = event.entity as? LivingEntity ?: return@on

            if (shouldHide(entity)) event.cancel()
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        deadTags.clear()
    }
}