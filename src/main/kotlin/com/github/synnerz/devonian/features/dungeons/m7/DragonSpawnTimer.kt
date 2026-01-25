package com.github.synnerz.devonian.features.dungeons.m7

import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.render.Render3DImmediate
import java.util.*

object DragonSpawnTimer : TextHudFeature(
    "dragonSpawnTimer",
    "",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "M7",
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
    )

    private var spawned = EnumSet.noneOf(M7Dragon::class.java)
    private var ticks = 0
    private var done = false

    override fun initialize() {
        on<ChatEvent> { event ->
            if (event.message == "[BOSS] Wither King: Incredible. You did what I couldn't do myself.") done = true
        }

        on<M7Events.DragonSpawned> { event ->
            if (done) return@on
            spawned.add(event.dragon)
            ticks = EventBus.serverTicks() + 100
        }

        on<ClientThreadServerTickEvent> {
            if (ticks <= 0) return@on

            val time = (ticks - EventBus.serverTicks()) * 0.05
            setLine("%.2fs".format(time))

            if (time <= 0) {
                ticks = 0
                spawned.clear()
            }
        }.setEnabled(SETTING_HUD.state)

        on<RenderOverlayEvent> { event ->
            if (ticks <= 0) return@on

            draw(event.ctx)
        }.setEnabled(SETTING_HUD.state)

        on<RenderWorldEvent> {
            if (ticks <= 0) return@on

            spawned.forEach {
                val time = (ticks - EventBus.serverTicks()) * 0.05

                if (time <= 0) {
                    ticks = 0
                    spawned.clear()
                }

                Render3DImmediate.renderString(
                    "${it.textColor}${it.colorName} §f%.2fs".format(time),
                    it.chin.x.toDouble(),
                    it.chin.y + 2.0,
                    it.chin.z.toDouble(),
                    10f,
                    maxDist = 100.0,
                    phase = true,
                )
            }
        }.setEnabled(SETTING_WORLD.state)
    }

    override fun getEditText(): List<String> = listOf((5000L - (System.currentTimeMillis() % 5000L)).toString())

    override fun onWorldChange(event: WorldChangeEvent) {
        spawned.clear()
        ticks = 0
        done = false
    }
}