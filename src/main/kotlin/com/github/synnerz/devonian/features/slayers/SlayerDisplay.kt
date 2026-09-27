package com.github.synnerz.devonian.features.slayers

import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.NameChangeEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.ScoreboardEvent
import com.github.synnerz.devonian.api.events.TickEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.StringUtils.colorCodes
import kotlinx.atomicfu.atomic
import net.minecraft.world.entity.LivingEntity

object SlayerDisplay : TextHudFeature(
    "slayerDisplay",
    "Displays a hud with your current slayer boss' stats",
    Categories.SLAYERS,
) {
    private val slayerRegex = "^Spawned by: (\\w+)$".toRegex()
    private val questStartedRegex = "^Slay the boss!$".toRegex()
    private val questCompletedRegex = "^Boss slain!$".toRegex()
    private val bossList = mutableListOf<SlayerBossData>()
    private var hasBossQuest = atomic(false)
//    private val carryingNames = mutableListOf<String>()
    // TODO: add carrying boss' support

    data class SlayerBossData(
        val standId: Int,
        val spawnedBy: String,
        val bossId: Int = standId - 3,
    )

    override fun initialize() {
        on<ScoreboardEvent> { event ->
            event.matches(questStartedRegex)?.let {
                hasBossQuest.value = true
            }
            if (event.matches(questCompletedRegex) == null) return@on

            hasBossQuest.value = false
        }

        on<NameChangeEvent> { event ->
            if (!hasBossQuest.value) return@on
            val name = event.name
            val match = slayerRegex.matchEntire(name)?.groupValues?.drop(1) ?: return@on

            Scheduler.scheduleTask {
                bossList.removeIf { it.standId == event.entityId || it.spawnedBy == match[0] }
                bossList.add(SlayerBossData(event.entityId, match[0]))
            }
        }

        on<TickEvent> {
            if (bossList.isEmpty() || !hasBossQuest.value) return@on
            val player = minecraft.player ?: return@on
            val world = minecraft.level ?: return@on
            val display = mutableListOf<String>()

            bossList.removeIf {
                val ( standId, spawnedBy, bossId ) = it
                if (spawnedBy != player.name.string/* && carryingNames.isEmpty()*/) return@removeIf false
                val spawnStand = world.getEntity(standId) ?: return@removeIf false
                val timeEntity = world.getEntity(standId - 1) ?: return@removeIf false
                val healthEntity = world.getEntity(standId - 2) ?: return@removeIf false
                val entity = world.getEntity(bossId) as? LivingEntity ?: return@removeIf false
                if (entity.isRemoved || entity.isDeadOrDying) {
                    return@removeIf true
                }

                display.add(timeEntity.customName?.colorCodes() ?: "",)
                display.add(spawnStand.customName?.colorCodes() ?: "")
                display.add(healthEntity.customName?.colorCodes() ?: "")
                false
            }

            clearLines()
            setLines(display)
        }

        on<RenderOverlayEvent> {
            draw(it.ctx)
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        bossList.clear()
        hasBossQuest.value = false
        clearLines()
    }

    override fun getEditText(): List<String> = listOf(
        "&c02:59",
        "&eSpawned by: &b${minecraft.player?.name?.string ?: ""}",
        "&c☠ &bVoidgloom Seraph IV &e64.2M&c❤"
    )
}