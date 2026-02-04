package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.events.ClientContainerCloseEvent
import com.github.synnerz.devonian.api.events.PickupItemInventoryEvent
import com.github.synnerz.devonian.api.events.ServerContainerCloseEvent
import com.github.synnerz.devonian.api.events.ServerContainerOpenEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import net.minecraft.world.item.Items

object PartyFinderRightClick : Feature(
    "partyFinderRightClick",
    "Sends the selected command whenever you right click on a party finder party",
    Categories.DUNGEONS,
    subcategory = "QOL"
) {
    private val SETTING_MODE = addSelection(
        "rightClickMode",
        0,
        listOf("CopyLeader", "LeaveParty"),
        "CopyLeader = copies the party's leader name, LeaveParty = leaves your current party",
        "PFRC Mode"
    )
    private val nameRegex = "^(\\w{1,16})'s Party$".toRegex()
    private var inPF = false

    override fun initialize() {
        on<ServerContainerOpenEvent> { event ->
            inPF = event.titleStr == "Party Finder"
        }

        on<ServerContainerCloseEvent> {
            inPF = false
        }

        on<ClientContainerCloseEvent> {
            inPF = false
        }

        on<PickupItemInventoryEvent> { event ->
            if (!event.isSplitItem || !inPF) return@on
            val slot = event.slot
            val itemStack = slot.item
            if (slot.container == minecraft.player?.inventory) return@on
            if (slot.containerSlot >= 45 || itemStack.item != Items.PLAYER_HEAD) return@on

            event.cancel()
            when (SETTING_MODE.get()) {
                0 -> {
                    val name = itemStack.customName?.string ?: return@on
                    val match = nameRegex.matchEntire(name)?.groupValues?.drop(1) ?: return@on
                    val leader = match.firstOrNull() ?: return@on

                    minecraft.keyboardHandler.clipboard = leader
                    ChatUtils.sendMessage("&bCopied leader name &a$leader", true)
                }
                1 -> {
                    ChatUtils.command("p leave")
                }
            }
        }
    }
}