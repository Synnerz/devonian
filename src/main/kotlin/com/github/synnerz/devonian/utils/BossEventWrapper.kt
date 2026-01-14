package com.github.synnerz.devonian.utils

import net.minecraft.client.gui.components.LerpingBossEvent
import net.minecraft.network.chat.Component
import net.minecraft.world.BossEvent
import java.util.*

class BossEventWrapper(val orig: LerpingBossEvent, var ov: Component) : LerpingBossEvent(
    null,
    null,
    0f,
    null,
    null,
    false,
    false,
    false,
) {
    override fun getName(): Component = ov

    override fun getId(): UUID? = orig.id
    override fun setName(component: Component) {}
    override fun getProgress(): Float = orig.progress
    override fun setProgress(f: Float) {}
    override fun getColor(): BossBarColor? = orig.color
    override fun setColor(bossBarColor: BossBarColor) {}
    override fun getOverlay(): BossBarOverlay? = orig.overlay
    override fun setOverlay(bossBarOverlay: BossBarOverlay) {}
    override fun shouldDarkenScreen(): Boolean = orig.shouldDarkenScreen()
    override fun setDarkenScreen(bl: Boolean): BossEvent = apply {}
    override fun shouldPlayBossMusic(): Boolean = orig.shouldPlayBossMusic()
    override fun setPlayBossMusic(bl: Boolean): BossEvent = apply {}
    override fun shouldCreateWorldFog(): Boolean = orig.shouldCreateWorldFog()
    override fun setCreateWorldFog(bl: Boolean): BossEvent = apply {}
}