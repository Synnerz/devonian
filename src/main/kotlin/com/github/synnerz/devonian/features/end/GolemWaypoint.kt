package com.github.synnerz.devonian.features.end

import com.github.synnerz.barrl.Context
import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.api.events.SubAreaEvent
import com.github.synnerz.devonian.api.events.TabUpdateEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.Location
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.SkullBlock
import java.awt.Color

// FIXME: whenever the server lags the scanner lags behind and does not scan properly
//  change this to be packet based instead later on.
object GolemWaypoint : Feature(
    "golemWaypoint",
    "Sets a waypoint to where the golem should spawn",
    "end",
    "the end"
) {
    private var protectorRegex = "^ Protector: (\\w+)$".toRegex()
    private val golemStages = listOf("Resting", "Dormant", "Agitated", "Disturbed", "Awakening", "Summoned")
    // Y level is always 4
    private val golemCoords = listOf(
        listOf(-644, -269),
        listOf(-689, -273),
        listOf(-727, -284),
        listOf(-678, -332),
        listOf(-649, -219),
        listOf(-639, -328),
    )
    var pos: BlockPos? = null
    var delegateScan = false
    var currentStage = 0

    override fun initialize() {
        on<TabUpdateEvent> { event ->
            if (Location.subarea == null) return@on
            val ( stage ) = event.matches(protectorRegex) ?: return@on
            val idx = golemStages.indexOf(stage)
            ChatUtils.sendMessage("&aGolem Stage is &b$stage &7($idx)", true)
            if (idx <= 0) {
                pos = null
                return@on
            }

            currentStage = idx
            delegateScan = !Location.subarea!!.contains("dragon's nest")
            if (delegateScan) return@on

            findPos()
        }

        on<SubAreaEvent> { event ->
            if (!delegateScan) return@on
            val subarea = event.subarea ?: return@on
            if (!subarea.contains("Dragon's Nest")) return@on

            findPos()
            delegateScan = false
        }

        on<RenderWorldEvent> {
            if (pos == null) return@on

            Context.Immediate?.renderWaypoint(
                pos!!.x.toDouble(), pos!!.y.toDouble(), pos!!.z.toDouble(),
                Color.CYAN,
                "§6${golemStages[currentStage]} §7($currentStage)",
                true,
                true
            )
        }

        on<WorldChangeEvent> {
            pos = null
            delegateScan = false
            currentStage = 0
        }
    }

    private fun findPos() {
        for (coord in golemCoords) {
            val ( x, z ) = coord
            val bp = BlockPos(x, 4 + currentStage, z)
            val state = minecraft.level?.getBlockState(bp) ?: continue
            if (state.block is SkullBlock) {
                pos = bp
                break
            }
        }
    }
}