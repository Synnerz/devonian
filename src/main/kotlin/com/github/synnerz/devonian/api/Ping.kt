package com.github.synnerz.devonian.api

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.events.*
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundAwardStatsPacket
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentSkipListSet

object Ping {
    private var didBeat = true
    private var lastBeat = 0.0
    private val awaitingBlockUpdate = mutableMapOf<BlockPos, Double>()

    private val samples = ConcurrentLinkedQueue<PingSample>()
    private var pingSum = atomic(0.0)
    private var weightSum = atomic(0)
    private var medianMax = ConcurrentSkipListSet<PingSample> { a, b -> b.v.compareTo(a.v).let { if (it == 0) a.t.compareTo(b.t) else it } }
    private var medianMin = ConcurrentSkipListSet<PingSample> { a, b -> a.v.compareTo(b.v).let { if (it == 0) a.t.compareTo(b.t) else it } }

    data class PingSample(val t: Double, val v: Double, val w: Int)

    private fun getTimeMS(): Double = System.nanoTime() / 1.0e6

    fun getLastPing(): Double = samples.lastOrNull()?.v ?: 0.0

    fun getAveragePing(): Double {
        val w = weightSum.value
        if (w == 0) return 0.0
        return pingSum.value / w
    }

    fun getMedianPing(): Double {
        val maxL = medianMax.size
        val minL = medianMin.size
        if (maxL > minL) return medianMax.first.v
        if (minL > maxL) return medianMin.first.v
        if (maxL == 0) return 0.0
        return 0.5 * (medianMax.first.v + medianMin.first.v)
    }

    private fun rebalanceHeaps() {
        while (medianMax.size - medianMin.size > 1) {
            val s = medianMax.pollFirst() ?: break
            medianMin.add(s)
        }
        while (medianMin.size - medianMax.size > 1) {
            val s = medianMin.pollFirst() ?: break
            medianMax.add(s)
        }
    }

    fun addSample(from: Double, weight: Int) {
        val t = getTimeMS()
        val ping = t - from
        val sample = PingSample(t, ping, weight)

        pingSum.update { it + ping * weight }
        weightSum.plusAssign(weight)
        samples.add(sample)

        if (ping > getMedianPing()) medianMin.add(sample)
        else medianMax.add(sample)
        rebalanceHeaps()
    }

    init {
        EventBus.on<PacketSentEvent> { event ->
            when (val packet = event.packet) {
                is ServerboundClientCommandPacket -> {
                    if (packet.action != ServerboundClientCommandPacket.Action.REQUEST_STATS) return@on
                    val t = getTimeMS()
                    if (!didBeat && lastBeat + 10_000.0 > t) event.ci.cancel()
                    else {
                        lastBeat = t
                        didBeat = false
                    }
                }

                is ServerboundUseItemOnPacket -> {
                    awaitingBlockUpdate[packet.hitResult.blockPos] = getTimeMS()
                }
            }
        }

        EventBus.on<PacketReceivedEvent> { event ->
            when (val packet = event.packet) {
                is ClientboundAwardStatsPacket -> {
                    if (didBeat) return@on
                    if (lastBeat == 0.0) return@on

                    addSample(lastBeat, 10)
                    didBeat = true
                }

                is ClientboundBlockUpdatePacket -> {
                    val t = awaitingBlockUpdate.remove(packet.pos)
                    if (t != null) addSample(t, 1)
                }
            }
        }

        EventBus.on<TickEvent> {
            val t = getTimeMS()
            var deltaPing = 0.0
            var deltaWeight = 0

            while (samples.isNotEmpty() && samples.peek().t < t - 60_000.0) {
                val sample = samples.poll()
                if (sample != null) {
                    deltaPing += sample.v
                    deltaWeight += sample.w
                }
            }

            if (deltaWeight > 0) {
                pingSum.update { it + deltaPing * deltaWeight }
                weightSum.minusAssign(deltaWeight)
                rebalanceHeaps()
            }

            if (
                t - lastBeat > (if (didBeat) 5_000.0 else 10_000.0)
            ) Devonian.minecraft.connection?.send(ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.REQUEST_STATS))
        }

        EventBus.on<WorldChangeEvent> {
            didBeat = true
            lastBeat = getTimeMS()
            awaitingBlockUpdate.clear()
        }
    }
}