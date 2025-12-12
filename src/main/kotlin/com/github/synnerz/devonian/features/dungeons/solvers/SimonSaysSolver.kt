package com.github.synnerz.devonian.features.dungeons.solvers

import com.github.synnerz.barrl.Context
import com.github.synnerz.devonian.api.events.BlockInteractEvent
import com.github.synnerz.devonian.api.events.PacketReceivedEvent
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
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
    private val SETTING_BLOCK_INCORRECT = addSelection(
        "blockClicks",
        0,
        listOf("Never", "Always", "WhenCrouching", "ExceptWhenCrouching"),
        "",
        "Block Incorrect Hits",
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

    private fun isValidButtonLocation(pos: BlockPos) = pos.x == 110 && pos.y in 120 .. 123 && pos.z in 92 .. 95

    private fun onSeaLantern(pos: BlockPos): Boolean {
        if (pos.x != 111) return false
        if (!isValidButtonLocation(pos)) return false
        if (!solution.contains(pos)) solution.add(pos)
        return true
    }

    override fun initialize() {
        on<PacketReceivedEvent> { event ->
            when (val packet = event.packet) {
                is ClientboundBlockUpdatePacket -> {
                    if (packet.blockState.block !== Blocks.SEA_LANTERN) return@on
                    onSeaLantern(packet.pos)
                }

                is ClientboundSectionBlocksUpdatePacket -> {
                    var firstPos: BlockPos? = null
                    var firstState: BlockState? = null
                    var foundLantern = false
                    packet.runUpdates { pos, state ->
                        if (firstPos == null) {
                            firstPos = pos
                            firstState = state
                        }
                        if (state.block === Blocks.SEA_LANTERN && onSeaLantern(pos)) foundLantern = true
                    }
                    if (foundLantern) return@on

                    val pos = firstPos ?: return@on
                    val state = firstState ?: return@on
                    if (pos.x != 110 || pos.y != 121 || pos.z != 94) return@on
                    if (!state.isAir) return@on
                    solution.clear()
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
                    pos.x - cam.x,
                    pos.y - cam.y,
                    pos.z - cam.z,
                    wire,
                    true,
                    SETTING_LINE_WIDTH.get()
                )
                Context.Immediate?.renderFilledShape(
                    BUTTON_SHAPE,
                    pos.x - cam.x,
                    pos.y - cam.y,
                    pos.z - cam.z,
                    fill,
                    true
                )
            }
        }

        on<BlockInteractEvent> { event ->
            if (solution.isEmpty()) return@on

            val pos = event.pos
            if (!isValidButtonLocation(pos)) return@on

            if (solution[0] == pos) solution.removeFirstOrNull()
            else {
                if (shouldBlockClicks()) event.cancel()
                else {
                    solution.dropWhile { it != pos }
                    solution.removeFirstOrNull()
                }
            }
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        solution.clear()
    }
}