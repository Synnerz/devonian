package com.github.synnerz.devonian.features.bossbar

import com.github.synnerz.devonian.api.Location
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.features.dungeons.clear.WatcherBossBar
import com.github.synnerz.devonian.features.misc.HideUselessBossBar
import net.minecraft.client.gui.components.LerpingBossEvent
import net.minecraft.network.chat.Component

object BossBar {
    fun changeBarName(comp: Component, event: LerpingBossEvent): Component? {
        return when (Location.area) {
            "catacombs" -> {
                if (Dungeons.inBoss.value) BossBarHealth.modify(comp, event)
                else {
                    if (comp.string == "§c§lThe Watcher") WatcherBossBar.modify(comp, event)
                    else if (HideUselessBossBar.isEnabled()) null
                    else comp
                }
            }
            "kuudra" -> BossBarHealth.modify(comp, event)
            else -> {
                if (HideUselessBossBar.isEnabled()) null
                else comp
            }
        }
    }
}