package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.CustomSounds
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.Items

object EtherwarpSound : Feature(
    "etherwarpSound",
    "Changes the sound the etherwarp makes whenever you have etherwarped successfully. Customize it via /devonian etherwarpsound.",
    subcategory = "Tweaks",
) {
    private val customSound = CustomSounds.create("etherwarpSound", "minecraft:entity.ender_dragon.hurt")
    private val itemIds = setOf("ASPECT_OF_THE_END", "ASPECT_OF_THE_VOID", "ETHERWARP_CONDUIT")
    private var lastClick = -1

    override fun initialize() {
        customSound.registerCommand("etherwarpsound")

        on<UseItemEvent> { event ->
            onRightClick(event.hand)
        }

        on<UseItemOnEvent> { event ->
            onRightClick(event.hand)
        }

        on<SoundPlayEvent> { event ->
            if (
                event.sound != "minecraft:entity.ender_dragon.hurt" ||
                event.volume != 1f ||
                event.pitch != 0.53968257f ||
                customSound.soundEvent == SoundEvents.ENDER_DRAGON_HURT
            ) return@on
            if (lastClick == -1 || lastClick - EventBus.serverTicks() !in 0..14) return@on

            event.cancel()
            customSound.schedulePlayWithEvent(event, false)
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        lastClick = -1
    }

    private fun onRightClick(hand: InteractionHand) {
        val player = minecraft.player ?: return
        val itemStack = player.getItemInHand(hand)
        val sbId = ItemUtils.skyblockId(itemStack) ?: return
        if (sbId !in itemIds) return
        val requireSneak = itemStack.item == Items.DIAMOND_SHOVEL || itemStack.item == Items.DIAMOND_SWORD
        if (requireSneak && !player.isSteppingCarefully) return

        lastClick = EventBus.serverTicks(true)
    }
}