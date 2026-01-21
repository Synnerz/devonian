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
            val l = name.siblings.size
            if (l < 2) return@getOrPut name

            var i = 0
            var normalStarsComp = name.siblings[l - 2]
            var starsComp = name.siblings.last()
            var starsText = (starsComp.contents as? PlainTextContents.LiteralContents)?.text ?: return@getOrPut name

            if (starsText == "✦") {
                if (l < 3) return@getOrPut name
                i = 1
                normalStarsComp = name.siblings[l - 3]
                starsComp = name.siblings[l - 2]
                starsText = (starsComp.contents as? PlainTextContents.LiteralContents)?.text?.trim() ?: return@getOrPut name
            }

            val stars = masterStars.indexOf(starsText) + 1
            if (stars <= 0) return@getOrPut name

            val comp = MutableComponent.create(name.contents).withStyle(name.style)
            comp.siblings.addAll(name.siblings.subList(0, l - 2 - i))
            comp.siblings.add(Component.literal("✪".repeat(stars)).withStyle(starsComp.style))
            if (stars != 5) {
                comp.siblings.add(Component.literal("✪".repeat(5 - stars)).withStyle(normalStarsComp.style))
            }
            if (i > 0) {
                comp.siblings.add(Component.literal(" "))
                comp.siblings.addAll(name.siblings.subList(l - i, l))
            }

            return@getOrPut comp
        } ?: name
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        cache.clear()
    }
}