package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.PacketReceivedEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.hud.texthud.Alert
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.StringUtils
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import net.minecraft.world.item.Items

object QuiverDisplay : TextHudFeature("quiverDisplay") {
    private val SETTING_COLOR_AMOUNT = addSwitch(
        "colorAmount",
        "color the amount of arrows remaining based on number",
        "Color Arrow Count",
        false
    )
    private val SETTING_QUIVER_SIZE = addSelection(
        "quiverSize",
        "",
        "Quiver Size",
        listOf("Giant", "Large", "Medium"),
        0
    )
    private val SETTING_ALERT_BELOW = addSlider(
        "alertBelow",
        "sends a low arrow alert when the number of arrows drops below this amount",
        "Low Arrow Alert",
        0.0, 2880.0,
        50.0
    )

    // TODO: add quiver refill cost (but no one uses so)

    private val quiverSizes = listOf(
        5 * 9 * 64,
        4 * 9 * 64,
        3 * 9 * 64
    )
    // TODO: dont remove formatting on lore :pray:
    private val arrowColors = mapOf(
        "Flint Arrow" to "§f",
        "Reinforced Iron Arrow" to "§f",
        "Gold-tipped Arrow" to "§f",
        "Redstone-tipped Arrow" to "§a",
        "Emerald-tipped Arrow" to "§a",
        "Bouncy Arrow" to "§9",
        "Icy Arrow" to "§9",
        "Armorshred Arrow" to "§9",
        "Explosive Arrow" to "§9",
        "Glue Arrow" to "§9",
        "Nansorb Arrow" to "§9",
        "Magma Arrow" to "§5",
    )

    private val arrowRegex = "Active Arrow: ([A-Za-z ]+) \\((\\d+)\\)".toRegex()

    private var sentAlert = false

    override fun initialize() {
        on<PacketReceivedEvent> { event ->
            val item = when (val packet = event.packet) {
                is ClientboundContainerSetContentPacket -> {
                    if (packet.containerId != 0) return@on
                    packet.items.getOrNull(44)
                }
                is ClientboundContainerSetSlotPacket -> {
                    if (packet.containerId != 0) return@on
                    if (packet.slot != 44) return@on
                    packet.item
                }
                else -> null
            } ?: return@on

            val mcItem = item.item
            if (mcItem !== Items.FEATHER && mcItem !== Items.ARROW) return@on

            val lore = ItemUtils.lore(item) ?: return@on

            lore.forEach {
                val match = arrowRegex.find(it) ?: return@forEach
                val name = match.groupValues.getOrNull(1) ?: return@on
                val amount = match.groupValues.getOrNull(2)?.toIntOrNull() ?: return@on
                if (amount <= SETTING_ALERT_BELOW.get()) {
                    if (!sentAlert) Alert.show("&cLow on Arrows", 1000)
                    sentAlert = true
                } else sentAlert = false

                val amountFormat =
                    if (SETTING_COLOR_AMOUNT.get()) StringUtils.colorForNumber(
                        amount,
                        quiverSizes.getOrElse(SETTING_QUIVER_SIZE.get()) { 2880 }
                    ) else "&a"
                val text = "${arrowColors[name] ?: ""}$name &fx$amountFormat$amount"

                Scheduler.scheduleTask { setLine(text) }
            }
        }

        on<RenderOverlayEvent> { event ->
            draw(event.ctx)
        }
    }

    override fun getEditText(): List<String> = listOf("&5Flint Arrow &fx&26969")
}