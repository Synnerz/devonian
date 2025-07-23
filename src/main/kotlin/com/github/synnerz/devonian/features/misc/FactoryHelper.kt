package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.events.GuiCloseEvent
import com.github.synnerz.devonian.events.PacketReceivedEvent
import com.github.synnerz.devonian.events.RenderSlotEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.ItemUtils
import com.github.synnerz.devonian.utils.render.Render2D
import net.minecraft.item.Items
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket
import java.awt.Color

object FactoryHelper : Feature("factoryHelper") {
    private val nextCPSRegex = "^  \\+([\\d.]+)x? Chocolate per second$".toRegex()
    private val currentCPSRegex = "\\+([\\d.]+)x? Chocolate per".toRegex()
    private val chocolateCostRegex = "^([\\d,]+) Chocolate$".toRegex()
    private val currentProductionRegex = "^([\\d,.]+) per second".toRegex()
    private val stats = mutableMapOf<Int, RabbitStat>()
    private var inFactory = false
    private var chocolatePurse = 0.0
    private var currentProduction = 0.0
    private var bestSlot = -1

    data class RabbitStat(val cpsCost: Int, val cost: Double)

    override fun initialize() {
        on<PacketReceivedEvent> { event ->
            val packet = event.packet
            if (packet is OpenScreenS2CPacket) {
                inFactory = packet.name?.string == "Chocolate Factory"
                return@on
            }

            if (!inFactory || packet !is ScreenHandlerSlotUpdateS2CPacket) return@on
            val slot = packet.slot
            if (slot > 54) return@on

            val itemStack = packet.stack
            if (itemStack.item != Items.PLAYER_HEAD) return@on
            val name = itemStack.customName?.string ?: return@on
            val lore = ItemUtils.lore(itemStack) ?: return@on

            if (name.endsWith("Chocolate")) return@on chocoPurse(name, lore)
            if (name.startsWith("Coach Jack")) return@on coachStats(slot, lore)

            if (!name.startsWith("Rabbit")) return@on

            rabbitStats(slot, lore)
        }

        on<GuiCloseEvent> {
            inFactory = false
        }

        on<RenderSlotEvent> { event ->
            if (!inFactory) return@on
            if (bestSlot == -1) return@on
            val ctx = event.ctx
            val slot = event.slot

            if (slot.index == bestSlot && slot.inventory !== minecraft.player?.inventory) {
                Render2D.drawRect(
                    ctx,
                    event.slot.x, event.slot.y,
                    16, 16, Color.CYAN
                )
            }
        }
    }

    private fun findBest() {
        if (stats.isEmpty()) return

        var best = -1
        var bestStat = stats.values.first()

        stats.entries.forEach { (slot, stat) ->
            if (stat.cost > chocolatePurse) return@forEach
            if (bestStat.cpsCost < stat.cpsCost) return@forEach
            best = slot
            bestStat = stat
        }

        bestSlot = best
    }

    private fun chocoPurse(name: String, lore: List<String>) {
        chocolatePurse = chocolateCostRegex.matchEntire(name)?.groupValues?.get(1)?.replace(",", "")?.toDouble() ?: 0.0
        for (line in lore) {
            val match = currentProductionRegex.matchEntire(line) ?: continue
            currentProduction = match.groupValues[1].replace(",", "").toDouble()
        }
        findBest()
    }

    private fun coachStats(slot: Int, lore: List<String>) {
        var cost = 0.0

        for (line in lore) {
            val match = chocolateCostRegex.matchEntire(line) ?: continue
            cost = match.groupValues[1].replace(",", "").toDouble()
        }
        if (cost == 0.0) return

        // FIXME: this can possibly be use during Time Tower activation
        //  in which case it might give a false Chocolate Production
        val currentProd = currentProduction * 0.01
        stats[slot] = RabbitStat((cost / currentProd).toInt(), cost)
    }

    private fun rabbitStats(slot: Int, lore: List<String>) {
        var currentCps = 0
        var nextCps = 0
        var cost = 0.0

        for (line in lore) {
            val costMatch = chocolateCostRegex.matchEntire(line)
            if (costMatch != null) {
                cost = costMatch.groupValues[1].replace(",", "").toDouble()
                continue
            }

            val nextCPSMatch = nextCPSRegex.matchEntire(line)
            if (nextCPSMatch != null) {
                nextCps = nextCPSMatch.groupValues[1].replace(",", "").toInt()
                continue
            }

            // Match current cps at the end because it checks "globally" and it can match next cps
            val match = currentCPSRegex.find(line) ?: continue
            currentCps = match.groupValues[1].replace(",", "").toInt()
        }
        if (cost == 0.0) return

        stats[slot] = RabbitStat(cost.toInt() / (nextCps - currentCps), cost)
        findBest()
    }
}