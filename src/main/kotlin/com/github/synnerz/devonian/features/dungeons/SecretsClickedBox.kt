package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.barrl.Context
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.PacketSentEvent
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.features.Feature
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.entity.SkullBlockEntity
import java.awt.Color

object SecretsClickedBox : Feature(
    "secretsClickedBox",
    "Highlights the secrets you have clicked surrounding them with a box, if a chest secret for example is locked the color will change to red.",
    "Dungeons",
    "catacombs"
) {
    private val skullIds = listOf("e0f3e929-869e-3dca-9504-54c666ee6f23", "fed95410-aba1-39df-9b95-1d4f361eb66e")
    private val allowedBlocks = listOf("minecraft:chest", "minecraft:lever", "minecraft:trapped_chest")
    private val lockedChestRegex = "^That chest is locked!$".toRegex()
    private val SETTING_BLOCK_COLOR = addColorPicker(
        "blockColor",
        "",
        "Clicked Block Color",
        Color(0, 255, 255, 50).rgb
    )
    private val SETTING_LOCKED_BLOCK_COLOR = addColorPicker(
        "lockedBlockColor",
        "",
        "Locked Block Color",
        Color(255, 0, 0, 50).rgb
    )
    var clickedBlock: BlockPos? = null
    var wasLocked = false

    override fun initialize() {
        on<PacketSentEvent> { event ->
            val packet = event.packet
            if (packet !is ServerboundUseItemOnPacket) return@on
            val result = packet.hitResult
            val blockState = minecraft.level?.getBlockState(result.blockPos) ?: return@on
            val registryName = BuiltInRegistries.BLOCK.getKey(blockState.block)

            if (registryName.path == "player_head" && blockState.hasBlockEntity()) {
                val entityBlock = minecraft.level?.getBlockEntity(result.blockPos) ?: return@on
                if (entityBlock.type != BlockEntityType.SKULL) return@on
                val skullBlock = entityBlock as SkullBlockEntity
                val owner = skullBlock.ownerProfile ?: return@on
                val id = owner.partialProfile().id ?: return@on

                if (!skullIds.contains("$id")) return@on

                clickedBlock = entityBlock.blockPos
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
                if (wasLocked) SETTING_LOCKED_BLOCK_COLOR.getColor() else SETTING_BLOCK_COLOR.getColor(), true
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