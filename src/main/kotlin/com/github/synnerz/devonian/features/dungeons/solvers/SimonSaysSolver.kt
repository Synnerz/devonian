package com.github.synnerz.devonian.features.dungeons.solvers

import com.github.synnerz.barrl.Context
import com.github.synnerz.devonian.api.WorldUtils
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.BasicState
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.shapes.Shapes
import java.awt.Color
import java.util.concurrent.CopyOnWriteArrayList

object SimonSaysSolver : Feature(
    "simonSaysSolver",
    "Highlights the correct buttons to press",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "Solvers",
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.S1.isActiveState)
    }

    private val SETTING_BLOCK_INCORRECT = addSelection(
        "blockClicks",
        0,
        listOf("Never", "Always", "WhenCrouching", "ExceptWhenCrouching"),
        "",
        "Block Incorrect Hits",
    )
    private val SETTING_BLOCK_LAG = addSlider(
        "blockLag",
        0.0,
        0.0, 10.0,
        "blocks clicks if the previous click was within this number of server ticks",
        "Block Clicks if Lagging",
    )
    private val SETTING_CHIME_ON_CLICK = addSwitch(
        "chime",
        false,
        "plays sound if correct button was clicked",
        "Chime on Correct Button",
    )
    private val SETTING_LINE_WIDTH = addSlider(
        "lineWidth",
        2.0,
        0.0, 10.0,
        "",
        "Simon Says Line Width",
    )
    private val SETTING_COLOR_WIRE_1 = addColorPicker(
        "colorWire1",
        Color(0, 255, 0, 255).rgb,
        "",
        "Correct Button Outline Color",
    )
    private val SETTING_COLOR_FILL_1 = addColorPicker(
        "colorFill1",
        Color(0, 255, 0, 64).rgb,
        "",
        "Correct Button Fill Color",
    )
    private val SETTING_COLOR_WIRE_2 = addColorPicker(
        "colorWire2",
        Color(255, 255, 0, 255).rgb,
        "",
        "Next Button Outline Color",
    )
    private val SETTING_COLOR_FILL_2 = addColorPicker(
        "colorFill2",
        Color(255, 255, 0, 64).rgb,
        "",
        "Next Button Fill Color",
    )
    private val SETTING_COLOR_WIRE_3 = addColorPicker(
        "colorWire3",
        Color(255, 0, 0, 255).rgb,
        "",
        "Next Next Button Outline Color",
    )
    private val SETTING_COLOR_FILL_3 = addColorPicker(
        "colorFill3",
        Color(255, 0, 0, 64).rgb,
        "",
        "Next Next Button Fill Color",
    )

    private fun shouldBlockClicks() = when (SETTING_BLOCK_INCORRECT.getCurrent()) {
        "Always" -> true
        "WhenCrouching" -> minecraft?.player?.isShiftKeyDown ?: false
        "ExceptWhenCrouching" -> !(minecraft?.player?.isShiftKeyDown ?: true)
        else -> false
    }
    private val PREVENTED_SOUND = SoundEvents.NOTE_BLOCK_BASS
    private val LAG_SOUND = SoundEvents.NOTE_BLOCK_BASEDRUM
    private val CORRECT_SOUND = SoundEvents.NOTE_BLOCK_XYLOPHONE

    private val solution = CopyOnWriteArrayList<BlockPos>()
    private val BUTTON_SHAPE = 0.002.let { e ->
        Shapes.create(
            1 - 0.125 - e,
            0.375 - e,
            0.3125 - e,
            1 + e,
            0.625 + e,
            0.6875 + e
        )
    }
    private var wasStartButtonLast = false
    private var hasButtons = false
    private var lastSolClick = 0
    var solutionTotal = 0

    private fun isValidButtonLocation(pos: BlockPos) = pos.y in 120 .. 123 && pos.z in 92 .. 95

    private fun onSeaLantern(pos: BlockPos): Boolean {
        if (pos.x != 111) return false
        if (!isValidButtonLocation(pos)) return false
        val p = pos.west()
        if (solution.getOrNull(solution.size - 1) != p) solution.add(p)
        return true
    }

    override fun initialize() {
        on<PacketReceivedEvent> { event ->
            when (val packet = event.packet) {
                is ClientboundBlockUpdatePacket -> {
                    if (packet.blockState.block != Blocks.SEA_LANTERN) return@on
                    onSeaLantern(packet.pos)
                }

                is ClientboundSectionBlocksUpdatePacket -> {
                    var pop = 16
                    packet.runUpdates { pos, state ->
                        if (state.isAir && pos.x == 110 && isValidButtonLocation(pos) && --pop == 0) solution.clear()
                    }
                    packet.runUpdates { pos, state ->
                        if (state.block == Blocks.SEA_LANTERN) onSeaLantern(pos)
                    }
                }
            }
        }

        on<RenderWorldEvent> { event ->
            val cam = event.ctx.gameRenderer().mainCamera.position.reverse()
            solution.forEachIndexed { i, pos ->
                val wire = when (i) {
                    0 -> SETTING_COLOR_WIRE_1.getColor()
                    1 -> SETTING_COLOR_WIRE_2.getColor()
                    else -> SETTING_COLOR_WIRE_3.getColor()
                }
                val fill = when (i) {
                    0 -> SETTING_COLOR_FILL_1.getColor()
                    1 -> SETTING_COLOR_FILL_2.getColor()
                    else -> SETTING_COLOR_FILL_3.getColor()
                }

                Context.Immediate?.renderBoxShape(
                    BUTTON_SHAPE,
                    pos.x + cam.x,
                    pos.y + cam.y,
                    pos.z + cam.z,
                    wire,
                    false,
                    SETTING_LINE_WIDTH.get()
                )
                Context.Immediate?.renderFilledShape(
                    BUTTON_SHAPE,
                    pos.x + cam.x,
                    pos.y + cam.y,
                    pos.z + cam.z,
                    fill,
                    false,
                )
            }
        }

        on<BlockInteractEvent> { event ->
            val pos = event.pos

            if (pos.x != 110) return@on
            wasStartButtonLast = pos.y == 121 && pos.z == 91

            if (solution.isEmpty()) return@on
            if (!isValidButtonLocation(pos)) return@on

            if (solution.getOrNull(0) == pos) {
                val tick = EventBus.serverTicks()
                if (lastSolClick + SETTING_BLOCK_LAG.get() < tick) {
                    solution.removeFirstOrNull()
                    lastSolClick = tick
                    if (SETTING_CHIME_ON_CLICK.get()) minecraft.level?.playPlayerSound(
                        CORRECT_SOUND.value(),
                        SoundSource.MASTER,
                        1f, 0.5f,
                    )
                    return@on
                } else minecraft.level?.playPlayerSound(
                    LAG_SOUND.value(),
                    SoundSource.MASTER,
                    1f, 0.5f,
                )
            }

            if (shouldBlockClicks()) {
                event.cancel()
                minecraft.level?.playPlayerSound(
                    PREVENTED_SOUND.value(),
                    SoundSource.MASTER,
                    1f, 0.5f,
                )
            } else {
                do {
                    val v = solution.getOrNull(0) ?: break
                    if (v == pos) break
                    solution.removeFirstOrNull()
                } while (true)
                solution.removeFirstOrNull()
            }
        }

        on<ClientThreadServerTickEvent> {
            val pos = BlockPos(110, 120, 92)
            if (!WorldUtils.isChunkLoaded(pos.x, pos.z)) return@on

            if (WorldUtils.getBlockState(pos.x, pos.y, pos.z)?.block == Blocks.STONE_BUTTON) {
                if (wasStartButtonLast) {
                    wasStartButtonLast = false
                    val kept = when (solution.size) {
                        0 -> 0
                        1 -> 1
                        in 2 .. 3 -> 2
                        in 4 .. 6 -> 3
                        in 7 .. 9 -> 4
                        else -> 5
                    }
                    while (solution.size > kept) solution.removeFirstOrNull()
                }
                if (!hasButtons) solutionTotal = solution.size
                hasButtons = true
            } else hasButtons = false
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        solution.clear()
        wasStartButtonLast = false
        hasButtons = false
        lastSolClick = 0
        solutionTotal = 0
    }
}