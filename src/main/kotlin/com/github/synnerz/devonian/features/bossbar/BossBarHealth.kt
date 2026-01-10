package com.github.synnerz.devonian.features.bossbar

import com.github.synnerz.devonian.features.Feature
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.components.LerpingBossEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.contents.PlainTextContents

object BossBarHealth : Feature("bossBarShowHealth", subcategory = "Tweaks") {
    private val STYLE1 = Style.EMPTY.withColor(ChatFormatting.DARK_GRAY)
    private val STYLE2 = Style.EMPTY.withColor(ChatFormatting.LIGHT_PURPLE)

    fun modify(comp: Component, event: LerpingBossEvent): Component? {
        if (!isEnabled()) return comp

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