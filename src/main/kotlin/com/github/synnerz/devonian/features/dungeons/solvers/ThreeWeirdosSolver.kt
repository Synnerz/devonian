package com.github.synnerz.devonian.features.dungeons.solvers

import com.github.synnerz.barrl.Context
import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.WorldUtils
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.features.Feature
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import java.awt.Color
import kotlin.math.round

object ThreeWeirdosSolver : Feature(
    "threeWeirdosSolver",
    "Highlights the correct chest in the three weirdos puzzle room as well as changing the color of the text in chat",
    "Dungeons",
    "catacombs"
) {
    private val npcRegex = "^\\[NPC] (\\w+): (.*)".toRegex()
    private val completedRegex = "^PUZZLE SOLVED! \\w+ wasn't fooled by \\w+! Good job!$".toRegex()
    private val failedRegex = "^PUZZLE FAIL! \\w+ was fooled by \\w+! Yikes!$".toRegex()
    private val solutions = listOf(
        "The reward is not in my chest!".toRegex(),
        "At least one of them is lying, and the reward is not in \\w+'s chest.?".toRegex(),
        "My chest doesn't have the reward\\. We are all telling the truth.?".toRegex(),
        "My chest has the reward and I'm telling the truth!".toRegex(),
        "The reward isn't in any of our chests.?".toRegex(),
        "Both of them are telling the truth\\. Also, \\w+ has the reward in their chest.?".toRegex()
    )
    private val dirs = listOf(
        1 to 0,
        -1 to 0,
        0 to 1,
        0 to -1
    )
    private val FILLED_COLOR = Color(0, 255, 0, 80)
    private val entityList = mutableMapOf<String, Int>()
    var currentChest: Pair<Double, Double>? = null
    var currentEntity: Vec3? = null

    override fun initialize() {
        on<ChatEvent> { event ->
            event.matches(completedRegex)?.let {
                reset()
                return@on
            }

            event.matches(failedRegex)?.let {
                reset()
                return@on
            }

            val match = event.matches(npcRegex) ?: return@on
            val ( name, message ) = match
            if (!solutions.any { it.matches(message) }) return@on

            event.cancel()
            ChatUtils.sendMessage("&e[NPC] &b&l$name: &a&l$message")
            getChest(name)
        }

        on<PacketNameChangeEvent> { event ->
            entityList[event.name] = event.entityId
        }

        on<RenderWorldEvent> {
            if (currentChest == null) return@on
            Context.Immediate?.renderBox(
                currentChest!!.first, 69.0, currentChest!!.second,
                color = Color.GREEN,
                phase = true
            )
            Context.Immediate?.renderFilledBox(
                currentChest!!.first, 69.0, currentChest!!.second,
                color = FILLED_COLOR,
                phase = true
            )
            if (currentEntity == null) return@on
            Context.Immediate?.renderFilledBox(
                currentEntity!!.x - 0.5, currentEntity!!.y, currentEntity!!.z - 0.5,
                0.8, 2.0,
                FILLED_COLOR,
                true
            )
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        reset()
    }

    private fun reset() {
        currentChest = null
        currentEntity = null
        entityList.clear()
    }

    private fun getChest(name: String) {
        val entityId = entityList[name] ?: return
        val entity = minecraft.level?.getEntity(entityId) ?: return
        val pos = entity.position()
        val x0 = pos.x
        val z0 = pos.z
        currentEntity = pos

        for (dir in dirs) {
            val ( dx, dz ) = dir
            val blockAt = WorldUtils.getBlockState(round(x0 + dx), 69.0, round(z0 + dz)) ?: continue
            if (blockAt.block != Blocks.CHEST) continue
            currentChest = round(x0 + dx) to round(z0 + dz)
        }
    }
}