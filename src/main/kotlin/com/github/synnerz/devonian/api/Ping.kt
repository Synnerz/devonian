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
import net.minecraft.network.protocol.ping.ClientboundPongResponsePacket
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentSkipListSet

object Ping {
    private var didBeat = true
    private var lastBeat = 0.0
    // lazy fix
    private val awaitingBlockUpdate = mutableMapOf<BlockPos, BlockEntry>()

    private data class BlockEntry(val time: Double, val expire: Double = time + 1000.0, var used: Boolean = false)

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

    fun addSample(ping: Double, weight: Int, t: Double) {
        if (ping > 1.0e6) return
        val sample = PingSample(t, ping, weight)

        pingSum.update { it + ping * weight }
        weightSum.plusAssign(weight)
        samples.add(sample)

        if (ping > getMedianPing()) medianMin.add(sample)
        else medianMax.add(sample)
        rebalanceHeaps()
    }

    private fun addSample_(from: Double, weight: Int) {
        val t = getTimeMS()
        addSample(t - from, weight, t)
    }

    init {
        EventBus.on<PacketSentEvent> { event ->
            when (val packet = event.packet) {
                is ServerboundClientCommandPacket -> {
                    if (packet.action != ServerboundClientCommandPacket.Action.REQUEST_STATS) return@on
                    val t = getTimeMS()
                    if (!didBeat && lastBeat + 10_000.0 > t) event.cancel()
                    else {
                        lastBeat = t
                        didBeat = false
                    }
                }

                is ServerboundUseItemOnPacket -> {
                    val t = getTimeMS()
                    awaitingBlockUpdate.merge(packet.hitResult.blockPos, BlockEntry(t)) { t, u ->
                        if (u.time > t.expire) u
                        else t
                    }
                }
            }
        }

        EventBus.on<PacketReceivedEvent> { event ->
            when (val packet = event.packet) {
                is ClientboundAwardStatsPacket -> {
                    if (didBeat) return@on
                    if (lastBeat == 0.0) return@on

                    addSample_(lastBeat, 10)
                    didBeat = true
                }

                is ClientboundBlockUpdatePacket -> {
                    val e = awaitingBlockUpdate[packet.pos] ?: return@on
                    if (e.used) return@on
                    addSample_(e.time, 1)
                    e.used = true
                }

                is ClientboundPongResponsePacket -> {
                    addSample((System.currentTimeMillis() - packet.time).toDouble(), 5, getTimeMS())
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
                    if (!medianMax.remove(sample)) medianMin.remove(sample)
                    deltaPing += sample.v
                    deltaWeight += sample.w
                }
            }

            if (deltaWeight > 0) {
                pingSum.update { it - deltaPing * deltaWeight }
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