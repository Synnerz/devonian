package com.github.synnerz.devonian.features.dungeons.solvers

import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.ClientBlockUpdateEvent
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.hud.texthud.Alert
import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.render.Render3DImmediate
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import java.awt.Color
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.math.abs

object SharpShooterSolver : Feature(
    "sharpShooterSolver",
    "Highlights the block you've already hit for 4th device.",
    Categories.F7,
    "catacombs",
    searchTags = setOf("i4", "device"),
    subcategory = "Solvers",
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.Terminals.isActiveState)
    }

    private val SETTING_SHOW_ALERT = addSwitch(
        "showAlert",
        true,
        "Displays an alert whenever you finish the device",
        "SharpShooter Alert"
    )
    private val SETTING_PLAY_SOUND = addSwitch(
        "playSound",
        true,
        "Makes the alert play a sound (ONLY WORKS IF ALERT IS ENABLED)",
        "SharpShooter Sound"
    )
    private val SETTING_USE_SCANNER = addSwitch(
        "useScanner",
        false,
        "Uses the scanner to check if 8 emerald blocks have been displayed, this is not enabled by default as this may be inaccurate so use at your own will",
        "SharpShooter Alert Scanner"
    )
    private val deviceCompletedRegex = "^(\\w{1,16}) completed a device! \\(\\d/7\\)$".toRegex()
    private val emeraldPositions = listOf(
        SolverPosition(68, 130, 50),
        SolverPosition(66, 130, 50),
        SolverPosition(64, 130, 50),
        SolverPosition(68, 128, 50),
        SolverPosition(66, 128, 50),
        SolverPosition(64, 128, 50),
        SolverPosition(68, 126, 50),
        SolverPosition(66, 126, 50),
        SolverPosition(64, 126, 50),
    )
    private val basePosition = SolverPosition(63, 127, 35)
    private val whitelist = CopyOnWriteArraySet<SolverPosition>()
    private var sentAlert = false

    private data class SolverPosition(val x: Int, val y: Int, val z: Int, var hit: Boolean = false)

    override fun initialize() {
        on<ChatEvent> { event ->
            event.matches(deviceCompletedRegex)?.let {
                val playerName = minecraft.player?.name?.string ?: return@on
                if (it[0] != playerName) return@on
                if (whitelist.size >= 9) {
                    if (SETTING_SHOW_ALERT.get())
                        Alert.show("&aSharpShooter Done", 1500, SETTING_PLAY_SOUND.get())
                    whitelist.clear()
                }
                return@on
            }
        }

        on<ClientBlockUpdateEvent> { event ->
            if (!Dungeons.inBoss.value || Dungeons.floor.floorNum != 7) return@on

            val oldBlockState = event.oldBlockState
            val blockState = event.blockState
            val block = blockState.block
            val blockPos = event.blockPos

            if (oldBlockState.block == Blocks.EMERALD_BLOCK && block == Blocks.BLUE_TERRACOTTA) {
                onBlueTerracotta(blockPos)
                return@on
            }
            if (block != Blocks.EMERALD_BLOCK) return@on

            onEmeraldBlock(blockPos)
        }

        on<RenderWorldEvent> {
            if (!Dungeons.inBoss.value || Dungeons.floor.floorNum != 7) return@on
            if (whitelist.isEmpty()) return@on

            whitelist.forEach {
                Render3DImmediate.renderFilledBox(
                    it.x.toDouble(), it.y.toDouble(), it.z.toDouble(),
                    1.0, 1.0,
                    if (it.hit) Color(255, 0, 0, 80) else Color(0, 255, 0, 80),
                    false,
                )
            }

            if (whitelist.size == 8 && SETTING_SHOW_ALERT.get() && SETTING_USE_SCANNER.get() && !sentAlert) {
                sentAlert = false
                Alert.show("&aSharpShooter Done", 1500, SETTING_PLAY_SOUND.get())
            }
            if (whitelist.size >= 9) whitelist.clear()
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        whitelist.clear()
        sentAlert = false
    }

    private fun onEmeraldBlock(bp: BlockPos) {
        emeraldPositions.find { it.x == bp.x && it.y == bp.y && it.z == bp.z } ?: return

        val pos = SolverPosition(bp.x, bp.y, bp.z)
        if (whitelist.contains(pos)) return

        val player = minecraft.player ?: return
        val x1 = player.x.toInt()
        val y1 = player.y.toInt()
        val z1 = player.z.toInt()
        val dist = abs(basePosition.x - x1) + abs(basePosition.y - y1) + abs(basePosition.z - z1)
        if (dist > 2) return

        whitelist.add(pos)
    }

    private fun onBlueTerracotta(blockPos: BlockPos) {
        val cachedData = whitelist.find { it.x == blockPos.x && it.y == blockPos.y && it.z == blockPos.z } ?: return
        cachedData.hit = true
    }
}