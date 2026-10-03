package com.github.synnerz.devonian.features.dungeons.m7

import com.github.synnerz.devonian.api.Scheduler
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
    Categories.M7,
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
    )

    private var spawned = EnumMap<M7Dragon, Int>(M7Dragon::class.java)
    private var done = false

    override fun initialize() {
        on<ChatEvent> { event ->
            if (event.message == "[BOSS] Wither King: Incredible. You did what I couldn't do myself.") done = true
        }

        on<M7Events.DragonParticles> { event ->
            if (done) return@on

            Scheduler.scheduleTask {
                spawned[event.dragon.type] = event.dragon.spawnTick
            }
        }

        on<ClientThreadServerTickEvent> {
            when (spawned.size) {
                1 -> {
                    val ticks = spawned.values.firstOrNull() ?: return@on
                    setLine("%.2fs".format(ticks * 0.05))
                    if (ticks <= 0) spawned.clear()
                }
                else -> {
                    val lines = mutableListOf<String>()
                    spawned.entries.removeIf { (drag, ticks) ->
                        lines.add("${drag.textColor}${drag.displayName}: %.2fs".format(ticks * 0.05))
                        ticks <= 0
                    }
                    setLines(lines)
                }
            }
        }

        on<RenderOverlayEvent> { event ->
            if (spawned.isEmpty()) return@on

            draw(event.ctx)
        }.setEnabled(SETTING_HUD.state)

        on<RenderWorldEvent> {
            if (spawned.isEmpty()) return@on

            val sTicks = EventBus.serverTicks()
            spawned.forEach { (drag, ticks) ->
                val time = (ticks - sTicks) * 0.05

                Render3DImmediate.renderString(
                    "${drag.textColor}${drag.colorName} §f%.2fs".format(time),
                    drag.chin.x.toDouble(),
                    drag.chin.y + 2.0,
                    drag.chin.z.toDouble(),
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
        done = false
    }
}