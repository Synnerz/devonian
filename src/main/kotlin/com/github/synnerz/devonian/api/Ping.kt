package com.github.synnerz.devonian.api

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.events.*
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.StatisticsS2CPacket
import net.minecraft.util.math.BlockPos
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

    fun addSample(from: Double, weight: Int) {
        val t = getTimeMS()
        val ping = t - from
        val sample = PingSample(t, ping, weight)

        pingSum.update { it + ping }
        weightSum.plusAssign(weight)
        samples.add(sample)

        if (medianMax.size > medianMin.size) medianMin.add(sample)
        else medianMax.add(sample)
    }

    init {
        EventBus.on<PacketSentEvent> { event ->
            when (val packet = event.packet) {
                is ClientStatusC2SPacket -> {
                    if (packet.mode != ClientStatusC2SPacket.Mode.REQUEST_STATS) return@on
                    val t = getTimeMS()
                    if (!didBeat && lastBeat + 10_000.0 > t) event.ci.cancel()
                    else lastBeat = t
                }

                is PlayerInteractBlockC2SPacket -> {
                    awaitingBlockUpdate[packet.blockHitResult.blockPos] = getTimeMS()
                }
            }
        }

        EventBus.on<PacketReceivedEvent> { event ->
            when (val packet = event.packet) {
                is StatisticsS2CPacket -> {
                    if (didBeat) return@on
                    if (lastBeat == 0.0) return@on

                    addSample(lastBeat, 10)
                    didBeat = true
                }

                is BlockUpdateS2CPacket -> {
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
                pingSum.update { it + deltaPing }
                weightSum.minusAssign(deltaWeight)

                if (medianMax.size - medianMin.size > 1) medianMin.addAll(medianMax.take((medianMax.size - medianMin.size) / 2))
                if (medianMin.size - medianMax.size > 1) medianMax.addAll(medianMin.take((medianMin.size - medianMax.size) / 2))
            }

            if (
                t - lastBeat > (if (didBeat) 5_000.0 else 10_000.0)
            ) Devonian.minecraft.networkHandler?.sendPacket(ClientStatusC2SPacket(ClientStatusC2SPacket.Mode.REQUEST_STATS))
        }

        EventBus.on<WorldChangeEvent> {
            didBeat = true
            lastBeat = getTimeMS()
            awaitingBlockUpdate.clear()
        }
    }
}