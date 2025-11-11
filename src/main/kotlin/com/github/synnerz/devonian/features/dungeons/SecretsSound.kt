package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.PacketReceivedEvent
import com.github.synnerz.devonian.api.events.PacketSentEvent
import com.github.synnerz.devonian.api.events.SoundPlayEvent
import com.github.synnerz.devonian.features.Feature
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.block.entity.SkullBlockEntity
import net.minecraft.component.DataComponentTypes
import net.minecraft.entity.ItemEntity
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket
import net.minecraft.registry.Registries
import net.minecraft.sound.SoundEvents

object SecretsSound : Feature("secretsSound", "catacombs") {
    private val skullIds = listOf("e0f3e929-869e-3dca-9504-54c666ee6f23", "fed95410-aba1-39df-9b95-1d4f361eb66e")
    private val allowedBlocks = listOf("minecraft:chest", "minecraft:lever", "minecraft:trapped_chest")
    private val secretItems = listOf(
        "Healing VIII Splash Potion", "Healing Potion 8 Splash Potion", "Decoy",
        "Inflatable Jerry", "Spirit Leap", "Trap",
        "Training Weights", "Defuse Kit", "Dungeon Chest Key",
        "Treasure Talisman", "Revive Stone", "Architect's First Draft",
        "Candycomb"
    )
    private val lockedChestRegex = "^That chest is locked!$".toRegex()
    private val successSound = SoundEvents.ENTITY_BLAZE_HURT
    private val declineSound = SoundEvents.BLOCK_ANVIL_PLACE

    override fun initialize() {
        on<PacketReceivedEvent> { event ->
            val packet = event.packet
            if (packet !is ItemPickupAnimationS2CPacket) return@on

            val id = packet.entityId
            val entityIn = minecraft.world?.getEntityById(id) ?: return@on
            if (entityIn !is ItemEntity) return@on
            val itemStack = entityIn.stack
            val customName = itemStack.get(DataComponentTypes.CUSTOM_NAME)?.string
            if (!secretItems.contains(customName)) return@on

            playSound()
        }

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
                if (owner.uuid.isEmpty) return@on
                val id = owner.uuid.get()

                if (!skullIds.contains("$id")) return@on

                playSound()
                return@on
            }

            if (!allowedBlocks.contains("${registryName.namespace}:${registryName.path}")) return@on

            playSound()
        }

        on<ChatEvent> { event ->
            event.matches(lockedChestRegex) ?: return@on
            playSound(true)
        }

        on<SoundPlayEvent> { event ->
            if (event.volume != 0.1f) return@on
            if (event.sound == "minecraft:entity.bat.death" || event.sound == "minecraft:entity.bat.hurt") {
                event.cancel()
                playSound()
            }
        }
    }

    private fun playSound(declined: Boolean = false) {
        if (declined) {
            minecraft.player?.playSound(declineSound, 1f, 1f)
            return
        }
        minecraft.player?.playSound(successSound, 1f, 2f)
    }
}