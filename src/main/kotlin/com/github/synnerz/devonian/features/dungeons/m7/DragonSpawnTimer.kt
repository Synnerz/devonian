package com.github.synnerz.devonian.features.dungeons.m7

import com.github.synnerz.barrl.Context
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.BasicState
import java.util.*
import java.util.concurrent.ConcurrentHashMap

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
    private val dragons = ConcurrentHashMap<String, Int>()
    private var ticks = 0

    override fun initialize() {
        on<M7Events.DragonSpawned> { event ->
            spawned.add(event.dragon)
            ticks = EventBus.serverTicks() + 100
            dragons[event.dragon.colorName] = EventBus.serverTicks() + 100
        }

        on<RenderOverlayEvent> { event ->
            if (dragons.isEmpty()) return@on

            for (entry in dragons) {
                val name = entry.key
                val ticks = entry.value

                val time = (ticks - EventBus.serverTicks()) * 0.05
                addLine("$name %.2fs".format(time))

                if (time <= 0) {
                    spawned.removeIf { it.colorName == name }
                    dragons.remove(name)
                    removeLine(hud.lines.size - 1)
                }
            }

            draw(event.ctx)
        }.setEnabled(SETTING_HUD.state)

        on<RenderWorldEvent> {
            if (ticks <= 0) return@on

            spawned.forEach {
                val time = (ticks - EventBus.serverTicks()) * 0.05

                Context.Immediate?.renderString(
                    "${it.textColor}${it.colorName} &f%.2fs".format(time),
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

    override fun getEditText(): List<String> = listOf((5000L - (System.currentTimeMillis() % 5000L)).toString())

    override fun onWorldChange(event: WorldChangeEvent) {
        spawned.clear()
        ticks = 0
        dragons.clear()
    }
}