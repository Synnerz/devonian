package com.github.synnerz.devonian.api

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.events.*
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import net.minecraft.network.protocol.ping.ClientboundPongResponsePacket
import net.minecraft.network.protocol.ping.ServerboundPingRequestPacket
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentSkipListSet
import kotlin.math.min

object Ping {
    private var lastBeat = 0L

    private val samples = ConcurrentLinkedQueue<PingSample>()
    private var pingSum = atomic(0.0)
    private var weightSum = atomic(0)
    private var medianMax = ConcurrentSkipListSet<PingSample> { a, b -> b.v.compareTo(a.v).let { if (it == 0) a.t.compareTo(b.t) else it } }
    private var medianMin = ConcurrentSkipListSet<PingSample> { a, b -> a.v.compareTo(b.v).let { if (it == 0) a.t.compareTo(b.t) else it } }

    data class PingSample(val t: Long, val v: Double, val w: Int)

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

    fun addSample(ping: Double, weight: Int, t: Long) {
        if (ping > 1.0e6) return
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
                is ServerboundPingRequestPacket -> {
                    lastBeat = System.currentTimeMillis()
                }
            }
        }

        EventBus.on<PacketReceivedEvent> { event ->
            when (val packet = event.packet) {
                is ClientboundPongResponsePacket -> {
                    val t = System.currentTimeMillis()
                    val n = System.nanoTime()
                    val a = (n - packet.time) * 1e-6
                    val b = (t - packet.time).toDouble()
                    val c = (n / 1_000_000L - packet.time).toDouble()

                    val p = listOf(a, b, c).filter { it >= 0.0 }.minOrNull() ?: return@on
                    addSample(p, 1, t)
                }
            }
        }

        EventBus.on<TickEvent> {
            val t = System.currentTimeMillis()
            var deltaPing = 0.0
            var deltaWeight = 0

            while (samples.isNotEmpty() && samples.peek().t < t - 60_000L) {
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

            if (t - lastBeat > 1_000L) Devonian.minecraft.connection?.send(ServerboundPingRequestPacket(System.nanoTime()))
        }
    }
}