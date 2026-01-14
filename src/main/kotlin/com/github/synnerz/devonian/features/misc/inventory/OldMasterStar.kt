package com.github.synnerz.devonian.features.misc.inventory

import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.features.Feature
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.contents.PlainTextContents
import java.util.*

object OldMasterStar : Feature(
    "oldMasterStar",
    "§6✪✪✪✪✪§c➌§r -> §c✪✪✪§6✪✪",
    subcategory = "Inventory",
) {
    private val masterStars = listOf("➊", "➋", "➌", "➍", "➎")

    private val cache = IdentityHashMap<Component, Component>()

    fun transformName(name: Component): Component {
        return cache.getOrPut(name) {
            if (name.siblings.size < 2) return@getOrPut name
            val normalStarsComp = name.siblings[name.siblings.size - 2]
            val starsComp = name.siblings.last()
            val starsText = (starsComp.contents as? PlainTextContents.LiteralContents)?.text ?: return@getOrPut name
            val stars = masterStars.indexOf(starsText) + 1
            if (stars <= 0) return@getOrPut name

            val comp = MutableComponent.create(name.contents).withStyle(name.style)
            comp.siblings.addAll(name.siblings.subList(0, name.siblings.size - 2))
            comp.siblings.add(Component.literal("✪".repeat(stars)).withStyle(starsComp.style))
            if (stars != 5) {
                comp.siblings.add(Component.literal("✪".repeat(5 - stars)).withStyle(normalStarsComp.style))
            }

            return@getOrPut comp
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        cache.clear()
    }
}