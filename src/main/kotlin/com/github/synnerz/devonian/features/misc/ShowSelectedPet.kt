package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.features.Feature
import net.minecraft.world.item.Items
import java.awt.Color

object ShowSelectedPet : Feature(
    "showSelectedPet",
    "Highlights the selected pet while inside of pets menu",
    subcategory = "General",
) {
    private val SETTING_HIGHLIGHT_COLOR = addColorPicker(
        "highlightColor",
        Color.CYAN.rgb,
        "The highlight color to be used in show selected pet.",
        "Selected Pet Color",
    )
    private val petsMenuRegex = "^(\\(\\d+/\\d+\\) )?Pets$".toRegex()
    private var inPets = false
    private var currentPetSlot = -1

    override fun initialize() {
        on<ServerContainerOpenEvent> { event ->
            inPets = petsMenuRegex.matches(event.titleStr)
            currentPetSlot = -1
        }

        on<ServerContainerCloseEvent> {
            inPets = false
            currentPetSlot = -1
        }

        on<ClientContainerCloseEvent> {
            inPets = false
            currentPetSlot = -1
        }

        on<ServerContainerSetSlotEvent> { event ->
            if (!inPets || event.slot > 54) return@on

            if (event.slot == 10 && ItemUtils.skyblockId(event.itemStack) == "BEJEWELED_COLLAR") {
                inPets = false
                currentPetSlot = -1
                return@on
            }

            if (event.itemStack.item != Items.PLAYER_HEAD) return@on
            ItemUtils.lore(event.itemStack)?.let {
                for (line in it) {
                    if (line == "Click to despawn!") {
                        currentPetSlot = event.slot
                        break
                    }
                }
            }
        }

        on<RenderSlotEvent> { event ->
            if (!inPets || event.isInventory()) return@on
            val slot = event.slot
            if (slot.containerSlot != currentPetSlot) return@on

            event.ctx.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, SETTING_HIGHLIGHT_COLOR.get())
        }.prio = 30
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        inPets = false
        currentPetSlot = -1
    }
}