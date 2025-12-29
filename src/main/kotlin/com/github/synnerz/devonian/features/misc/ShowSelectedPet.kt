package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.events.PacketReceivedEvent
import com.github.synnerz.devonian.api.events.PacketSentEvent
import com.github.synnerz.devonian.api.events.RenderSlotEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.features.Feature
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import java.awt.Color

object ShowSelectedPet : Feature(
    "showSelectedPet",
    "Highlights the selected pet while inside of pets menu",
    subcategory = "General"
) {
    private val SETTING_HIGHLIGHT_COLOR = addColorPicker(
        "highlightColor",
        Color.CYAN.rgb,
        "The highlight color to be used in show selected pet",
        "Selected Pet Color"
    )
    private val petsMenuRegex = "^Pets(?: \\(\\d+/\\d+\\))? ?\$".toRegex()
    private const val SELECTED_PET_LORE = "Click to despawn!"
    private var inPets = false
    private var currentPetSlot = -1

    override fun initialize() {
        on<PacketReceivedEvent> { event ->
            val packet = event.packet
            when (packet) {
                is ClientboundOpenScreenPacket -> {
                    val title = packet.title.string
                    inPets = petsMenuRegex.matches(title)
                    currentPetSlot = -1
                    return@on
                }

                is ClientboundContainerClosePacket -> {
                    inPets = false
                    currentPetSlot = -1
                    return@on
                }

                is ClientboundContainerSetContentPacket -> {
                    val items = packet.items
                    val possiblyForge = items.getOrNull(10)
                    if (possiblyForge != null && ItemUtils.skyblockId(possiblyForge) == "BEJEWELED_COLLAR") {
                        inPets = false
                        currentPetSlot = -1
                        return@on
                    }
                    if (!inPets) return@on

                    for (idx in 0..<45) {
                        val itemStack = items.getOrNull(idx) ?: continue
                        if (!isSelectedPet(itemStack)) continue

                        currentPetSlot = idx
                    }
                }

                is ClientboundContainerSetSlotPacket -> {
                    if (!inPets) return@on
                    val slot = packet.slot
                    if (slot !in 0..<45) return@on

                    if (isSelectedPet(packet.item)) {
                        currentPetSlot = slot
                        return@on
                    }
                    if (slot == currentPetSlot) currentPetSlot = -1
                }
            }
        }

        on<PacketSentEvent> { event ->
            val packet = event.packet
            if (packet !is ServerboundContainerClosePacket) return@on

            inPets = false
            currentPetSlot = -1
        }

        on<RenderSlotEvent> { event ->
            if (!inPets) return@on
            val slot = event.slot
            if (slot.container == minecraft.player?.inventory) return@on
            if (currentPetSlot == -1 || slot.containerSlot != currentPetSlot) return@on

            event.ctx.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, SETTING_HIGHLIGHT_COLOR.get())
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        inPets = false
        currentPetSlot = -1
    }

    private fun isSelectedPet(itemStack: ItemStack): Boolean {
        if (itemStack.item != Items.PLAYER_HEAD || itemStack == ItemStack.EMPTY) return false
        val lore = ItemUtils.lore(itemStack) ?: return false
        return lore.any { it == SELECTED_PET_LORE }
    }
}
