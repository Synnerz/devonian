package com.github.synnerz.devonian.features.dungeons.m7

import com.github.synnerz.barrl.Context
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.ClientThreadServerTickEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.BasicState
import java.util.EnumSet

object DragonSpawnTimer : TextHudFeature(
    "dragonSpawnTimer",
    "",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "HUD",
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.WitherKing.isActiveState)
    }

    private val SETTING_HUD = addSwitch(
        "hud",
        true,
        "",
        "HUD Display",
    )
    private val SETTING_WORLD = addSwitch(
        "world",
        false,
        "",
        "Render Dragon Timer Under Chin",
        subcategory = "World",
    )

    private var spawned = EnumSet.noneOf(M7Dragon::class.java)
    private var ticks = 0

    override fun initialize() {
        on<M7Events.DragonSpawned> { event ->
            spawned.add(event.dragon)
            ticks = 100
        }

        on<ClientThreadServerTickEvent> {
            if (ticks <= 0) return@on
            ticks--
            if (ticks <= 0) spawned.clear()
        }

        on<RenderOverlayEvent> { event ->
            if (ticks <= 0) return@on

            setLine((ticks * 50).toString())
            draw(event.ctx)
        }.setEnabled(SETTING_HUD.state)

        on<RenderWorldEvent> {
            if (ticks <= 0) return@on

            spawned.forEach {
                Context.Immediate?.renderString(
                    (ticks * 50).toString(),
                    it.chin.x.toDouble(),
                    it.chin.y + 2.0,
                    it.chin.z.toDouble(),
                    10f,
                    backgroundBox = false,
                    increase = false,
                    phase = true,
                )
            }
        }.setEnabled(SETTING_WORLD.state)
    }

    override fun getEditText(): List<String> = listOf((System.currentTimeMillis() % 5000L).toString())

    override fun onWorldChange(event: WorldChangeEvent) {
        spawned.clear()
        ticks = 0
    }
}