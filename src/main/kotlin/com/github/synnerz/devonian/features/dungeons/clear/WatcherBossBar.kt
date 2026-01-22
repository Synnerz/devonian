package com.github.synnerz.devonian.features.dungeons.clear

import com.github.synnerz.devonian.api.dungeon.DungeonScanner
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.dungeon.mapEnums.RoomTypes
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.mixin.accessor.LerpingBossEventAccessor
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.components.LerpingBossEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.contents.PlainTextContents
import kotlin.math.roundToInt

object WatcherBossBar : Feature(
    "watcherBossBar",
    "",
    Categories.DUNGEONS,
    subcategory = "QOL",
) {
    private val SETTING_SHOW_PROGRESS = addSwitch(
        "showProgress",
        true,
        "show progress as fraction, e.g. 5/13",
        "Show Watcher Progress",
    )
    private val SETTING_HIDE_NOT_BLOOD = addSwitch(
        "hideNotBlood",
        true,
        "",
        "Hide When Not In Blood",
    )

    private val STYLE1 = Style.EMPTY.withColor(ChatFormatting.DARK_GRAY)
    private val STYLE2 = Style.EMPTY.withColor(ChatFormatting.LIGHT_PURPLE)

    fun modify(comp: Component, event: LerpingBossEvent): Component? {
        if (!isEnabled()) return comp

        if (SETTING_HIDE_NOT_BLOOD.get()) {
            if (DungeonScanner.currentRoom?.type != RoomTypes.BLOOD) return null
        }

        if (!SETTING_SHOW_PROGRESS.get()) return comp

        val f = (event as LerpingBossEventAccessor).targetPercent
        val total = Dungeons.floor.bloodMobs

        val clone = comp.copy()
        clone.siblings.add(MutableComponent.create(PlainTextContents.LiteralContents(" - ")).withStyle(STYLE1))
        clone.siblings.add(
            MutableComponent.create(
                PlainTextContents.LiteralContents("${(f * total).roundToInt()}/$total")
            ).withStyle(STYLE2)
        )

        return clone
    }
}