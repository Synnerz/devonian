package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.PacketReceivedEvent
import com.github.synnerz.devonian.api.events.PacketSentEvent
import com.github.synnerz.devonian.api.events.SoundPlayEvent
import com.github.synnerz.devonian.features.Feature
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.entity.SkullBlockEntity

object SecretsSound : Feature(
    "secretsSound",
    "Plays a sound whenever you click, pick up (a secret) or kill a bat (This also plays an anvil sound whenever the chest is locked)",
    "Dungeons",
    "catacombs"
) {
    private val skullIds = listOf("e0f3e929-869e-3dca-9504-54c666ee6f23", "fed95410-aba1-39df-9b95-1d4f361eb66e")
    private val allowedBlocks = listOf("minecraft:chest", "minecraft:lever", "minecraft:trapped_chest")
    private val secretItems = setOf(
        "Healing VIII Splash Potion", "Healing Potion 8 Splash Potion", "Decoy",
        "Inflatable Jerry", "Spirit Leap", "Trap",
        "Training Weights", "Defuse Kit", "Dungeon Chest Key",
        "Treasure Talisman", "Revive Stone", "Architect's First Draft",
        "Candycomb"
    )
    private val lockedChestRegex = "^That chest is locked!$".toRegex()
    private val successSound = SoundEvents.BLAZE_HURT
    private val declineSound = SoundEvents.ANVIL_PLACE

    override fun initialize() {
        on<PacketReceivedEvent> { event ->
            val packet = event.packet
            if (packet !is ClientboundTakeItemEntityPacket) return@on

            val id = packet.itemId
            val entityIn = minecraft.level?.getEntity(id) ?: return@on
            if (entityIn !is ItemEntity) return@on
            val itemStack = entityIn.item
            val customName = itemStack.get(DataComponents.CUSTOM_NAME)?.string
            if (!secretItems.contains(customName)) return@on

            playSound()
        }

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
                if (owner.id.isEmpty) return@on
                val id = owner.id.get()

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