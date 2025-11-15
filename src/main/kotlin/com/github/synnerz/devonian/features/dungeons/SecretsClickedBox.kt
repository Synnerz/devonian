package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.barrl.Context
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.PacketSentEvent
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.features.Feature
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.block.entity.SkullBlockEntity
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket
import net.minecraft.registry.Registries
import net.minecraft.util.math.BlockPos
import java.awt.Color

object SecretsClickedBox : Feature("secretsClickedBox", "catacombs") {
    private val skullIds = listOf("e0f3e929-869e-3dca-9504-54c666ee6f23", "fed95410-aba1-39df-9b95-1d4f361eb66e")
    private val allowedBlocks = listOf("minecraft:chest", "minecraft:lever", "minecraft:trapped_chest")
    private val lockedChestRegex = "^That chest is locked!$".toRegex()
    var filledBlockColor = Color(0, 255, 255, 50)
    var filledLockedBlockColor = Color(255, 0, 0, 50)
    var clickedBlock: BlockPos? = null
    var wasLocked = false

    override fun initialize() {
        on<PacketSentEvent> { event ->
            val packet = event.packet
            if (packet !is PlayerInteractBlockC2SPacket) return@on
            val result = packet.blockHitResult
            val blockState = minecraft.world?.getBlockState(result.blockPos) ?: return@on
            val registryName = Registries.BLOCK.getId(blockState.block)

            if (registryName.path == "player_head" && blockState.hasBlockEntity()) {
                val entityBlock = minecraft.world?.getBlockEntity(result.blockPos) ?: return@on
                if (entityBlock.type != BlockEntityType.SKULL) return@on
                val skullBlock = entityBlock as SkullBlockEntity
                val owner = skullBlock.owner ?: return@on
                if (owner.id.isEmpty) return@on
                val id = owner.id.get()

                if (!skullIds.contains("$id")) return@on

                clickedBlock = entityBlock.pos
                val thisClickedBlock = clickedBlock
                Scheduler.scheduleTask(20) {
                    if (clickedBlock == thisClickedBlock) clickedBlock = null
                }
                return@on
            }

            if (!allowedBlocks.contains("${registryName.namespace}:${registryName.path}")) return@on

            clickedBlock = result.blockPos
            val thisClickedBlock = clickedBlock
            Scheduler.scheduleTask(20) {
                if (clickedBlock != thisClickedBlock) return@scheduleTask
                clickedBlock = null
                wasLocked = false
            }
        }

        on<ChatEvent> { event ->
            event.matches(lockedChestRegex) ?: return@on
            wasLocked = true
        }

        on<RenderWorldEvent> {
            if (clickedBlock == null) return@on
            val immediate = Context.Immediate ?: return@on
            immediate.renderFilledBox(
                clickedBlock!!.x.toDouble(), clickedBlock!!.y.toDouble(), clickedBlock!!.z.toDouble(),
                if (wasLocked) filledLockedBlockColor else filledBlockColor, true
            )

            immediate.renderBox(
                clickedBlock!!.x.toDouble(), clickedBlock!!.y.toDouble(), clickedBlock!!.z.toDouble(),
                if (wasLocked) Color.RED else Color.CYAN, true
            )
        }

        on<WorldChangeEvent> {
            clickedBlock = null
            wasLocked = false
        }
    }
}