package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.Location
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.components.LerpingBossEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.contents.PlainTextContents

object BossBarHealth : Feature("bossBarShowHealth") {
    private val STYLE1 = Style.EMPTY.withColor(ChatFormatting.DARK_GRAY)
    private val STYLE2 = Style.EMPTY.withColor(ChatFormatting.LIGHT_PURPLE)

    fun changeBarName(comp: Component, event: LerpingBossEvent): Component? {
        if (!isEnabled()) return null
        when (Location.area) {
            // fuck watcher
            "catacombs" -> if (!Dungeons.inBoss.value) return null
            "kuudra" -> {}
            else -> return null
        }
        val clone = comp.copy()
        clone.siblings.add(MutableComponent.create(PlainTextContents.LiteralContents(" - ")).withStyle(STYLE1))
        clone.siblings.add(
            MutableComponent.create(
                PlainTextContents.LiteralContents("%.1f%%".format(event.progress * 100f))
            ).withStyle(STYLE2)
        )
        return clone
    }
}