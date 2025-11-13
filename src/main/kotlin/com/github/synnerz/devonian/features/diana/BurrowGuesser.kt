package com.github.synnerz.devonian.features.diana

import com.github.synnerz.barrl.Context
import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.Ping
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.features.WorldFeature
import com.github.synnerz.devonian.mixin.accessor.ClientPlayerEntityAccessor
import com.github.synnerz.devonian.utils.math.MathUtils
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.processNextEventInCurrentThread
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket
import net.minecraft.particle.ParticleTypes
import java.awt.Color
import java.util.*
import java.util.stream.Collector
import kotlin.math.*
import kotlin.random.Random


// Credits to https://github.com/hannibal002/SkyHanni/blob/beta/src/main/java/at/hannibal2/skyhanni/features/event/diana/PreciseGuessBurrow.kt
// & https://github.com/PerseusPotter/chicktils/blob/master/modules/diana.js
object BurrowGuesser : WorldFeature("burrowGuesser", "hub") {
    private val SETTING_GUESS_COLOR = Color.BLUE
    private val SETTING_OLD_GUESS_COLOR = Color(82, 14, 125)
    private const val SETTING_REMEMBER_PREVIOUS_GUESSES = true
    private val SETTING_PARTICLE_PATH_COLOR = Color.CYAN

    private val spadeUsePositions = LinkedList<PositionTime>()
    private val unclaimedParticles = mutableListOf<PositionTime>()
    private val possibleStartingParticles = mutableListOf<PositionTime>()
    private var knownChain = mutableListOf<PositionTime>()

    private const val MIN_CHAIN_LENGTH = 6
    private const val MAX_CHAIN_DISTANCE_ERROR = 0.5
    private const val RANSAC_ITERS_PER = 30

    private val guessPos = atomic<PositionTime?>(null)
    private val splinePoly = atomic<Array<(t: Double) -> Double>?>(null)
    private val particlePath = atomic(doubleArrayOf())

    data class PositionTime(val t: Int, val x: Double, val y: Double, val z: Double)

    fun resetGuess() {
        splinePoly.value = null
        particlePath.value = doubleArrayOf()

        if (SETTING_REMEMBER_PREVIOUS_GUESSES) {
            val guess = guessPos.value
            val player = minecraft.player
            if (
                guess != null && player != null &&
                (player.x - guess.x).pow(2) + (player.y - guess.y).pow(2) + (player.z - guess.z).pow(2) > 100 &&
                (if (guess.z < -30) -230 < guess.x else -300 < guess.x) && guess.x < 210 &&
                -240 < guess.z && guess.z < 210 &&
                50 < guess.y && guess.y < 120
            ) BurrowManager.addBurrow(
                BurrowManager.BurrowType.OLD_GUESS,
                guess.x,
                guess.y,
                guess.z
            )
        }

        guessPos.value = null
    }

    fun fullReset() {
        resetGuess()
        spadeUsePositions.clear()
        unclaimedParticles.clear()
        possibleStartingParticles.clear()
    }

    private fun isSpade(sbId: String): Boolean =
        sbId == "ANCESTRAL_SPADE" || sbId == "ARCHAIC_SPADE" || sbId == "DEIFIC_SPADE"

    private fun updateGuess() {
        if (knownChain.size < MIN_CHAIN_LENGTH) return

        val time = DoubleArray(knownChain.size) { it.toDouble() }
        val coeffX = MathUtils.polyRegression(3, time, DoubleArray(knownChain.size) { knownChain[it].x }) ?: return
        val coeffY = MathUtils.polyRegression(3, time, DoubleArray(knownChain.size) { knownChain[it].y }) ?: return
        val coeffZ = MathUtils.polyRegression(3, time, DoubleArray(knownChain.size) { knownChain[it].z }) ?: return
        val poly = arrayOf(
            MathUtils.toPolynomial(coeffX),
            MathUtils.toPolynomial(coeffY),
            MathUtils.toPolynomial(coeffZ)
        )
        splinePoly.value = poly

        val dx0 = coeffX[1]
        val dy0 = coeffY[1]
        val dz0 = coeffZ[1]
        val xz = hypot(dx0, dz0)

        val weight = sqrt(
            -24.0 * sin(
                MathUtils.convergeHalfInterval(
                    { x -> atan2(sin(x) - 0.75, cos(x)) },
                    -atan2(dy0, xz),
                    -PI / 2,
                    PI / 2,
                    true
                )
            ) + 25.0
        )
        val weightT = 3 * weight / sqrt(dx0 * dx0 + dy0 * dy0 + dz0 * dz0)
        // val distance = weightT * 1.9

        guessPos.value = PositionTime(0, poly[0](weightT), poly[1](weightT) - 1.0, poly[2](weightT))
        // TODO: renderLine
        /*
        particlePath.value = DoubleArray(300) {
            val i = it / 3
            val o = it % 3
            val t = MathUtils.rescale(i.toDouble(), 0.0, 99.0, 0.0, weightT)
            poly[o](t)
        }
        */
    }

    private fun shuffleUntil(arr: IntArray, limit: Int = arr.size) {
        for (i in 0 until limit) {
            val j = Random.nextInt(i, arr.size)
            val t = arr[i]
            arr[i] = arr[j]
            arr[j] = t
        }
    }

    private fun ransac() {
        val t = EventBus.totalTicks
        possibleStartingParticles.removeIf { it.t < t - 20 }
        unclaimedParticles.removeIf { it.t < t - 20 }

        val L1 = possibleStartingParticles.size
        val L2 = unclaimedParticles.size
        val L = L1 + L2
        if (L < MIN_CHAIN_LENGTH) return
        if (L1 == 0) return

        val comb =
            MathUtils.binomial(L, MIN_CHAIN_LENGTH) - (
            if (L - L2 < MIN_CHAIN_LENGTH) 0
            else MathUtils.binomial(L - L2, MIN_CHAIN_LENGTH)
            )
        val rand = IntArray(L - 1) { it }
        var start = 0

        var bestD = Double.POSITIVE_INFINITY
        var best = mutableListOf<PositionTime>()
        val tried = mutableSetOf<Int>()

        for (i in 0 until min(comb, RANSAC_ITERS_PER)) {
            shuffleUntil(rand, MIN_CHAIN_LENGTH - 1)
            start = Random.nextInt(0, L1)
            var hash = MathUtils.FIRST_100_PRIMES[start]
            for (j in 0 until MIN_CHAIN_LENGTH - 1) {
                hash *= MathUtils.FIRST_100_PRIMES[if (j > start) j + 1 else j]
            }
            if (!tried.add(hash)) continue

            val possInliers = Array(MIN_CHAIN_LENGTH) {
                if (it == 0) possibleStartingParticles[start]
                else {
                    val idx = if (it > start) it + 1 else it
                    if (idx < L1) possibleStartingParticles[idx] else unclaimedParticles[idx - L1]
                }
            }
            var minT = possInliers.minOf { it.t }
            var time = DoubleArray(MIN_CHAIN_LENGTH) { (possInliers[it].t - minT).toDouble() }
            var cX = MathUtils.polyRegression(3, time, DoubleArray(MIN_CHAIN_LENGTH) { possInliers[it].x }) ?: continue
            var cY = MathUtils.polyRegression(3, time, DoubleArray(MIN_CHAIN_LENGTH) { possInliers[it].y }) ?: continue
            var cZ = MathUtils.polyRegression(3, time, DoubleArray(MIN_CHAIN_LENGTH) { possInliers[it].z }) ?: continue
            var polyX = MathUtils.toPolynomial(cX)
            var polyY = MathUtils.toPolynomial(cY)
            var polyZ = MathUtils.toPolynomial(cZ)

            val inliers = mutableListOf<PositionTime>()
            val addIf: (PositionTime) -> Unit = { v ->
                val px = polyX((v.t - minT).toDouble())
                val py = polyY((v.t - minT).toDouble())
                val pz = polyZ((v.t - minT).toDouble())
                if (
                    (v.x - px).pow(2) +
                    (v.y - py).pow(2) +
                    (v.z - pz).pow(2) < 9
                ) inliers.add(v)
            }
            possibleStartingParticles.forEach(addIf)
            unclaimedParticles.forEach(addIf)

            minT = inliers.minOf { it.t }
            time = DoubleArray(inliers.size) { (inliers[it].t - minT).toDouble() }
            cX = MathUtils.polyRegression(3, time, DoubleArray(inliers.size) { inliers[it].x }) ?: continue
            cY = MathUtils.polyRegression(3, time, DoubleArray(inliers.size) { inliers[it].y }) ?: continue
            cZ = MathUtils.polyRegression(3, time, DoubleArray(inliers.size) { inliers[it].z }) ?: continue
            polyX = MathUtils.toPolynomial(cX)
            polyY = MathUtils.toPolynomial(cY)
            polyZ = MathUtils.toPolynomial(cZ)

            val d =
                inliers.sumOf { abs(it.x - polyX((it.t - minT).toDouble())) } +
                inliers.sumOf { abs(it.y - polyY((it.t - minT).toDouble())) } +
                inliers.sumOf { abs(it.z - polyZ((it.t - minT).toDouble())) }
            if (d < bestD) {
                bestD = d
                best = inliers
            }
        }

        if (bestD < MAX_CHAIN_DISTANCE_ERROR && best.size >= MIN_CHAIN_LENGTH) {
            val s = best.toSet()
            possibleStartingParticles.removeAll(s)
            unclaimedParticles.removeAll(s)

            resetGuess()
            best.sortBy { it.t }
            knownChain = best
            updateGuess()
        }
    }

    override fun initialize() {
        on<PacketReceivedEvent> { event ->
            val packet = event.packet
            if (
                packet !is ParticleS2CPacket ||
                packet.parameters.type != ParticleTypes.DRIPPING_LAVA ||
                packet.count != 2 ||
                packet.speed != -0.5f ||
                !packet.isImportant ||
                packet.offsetX != 0f ||
                packet.offsetY != 0f ||
                packet.offsetZ != 0f
            ) return@on

            val t = EventBus.totalTicks
            val x = packet.x
            val y = packet.y
            val z = packet.z
            val part = PositionTime(t, x, y, z)
            if (knownChain.size > 0 && t < knownChain.last().t + 5) {
                val spline = splinePoly.value
                if (spline != null) {
                    val predX = spline[0](knownChain.size.toDouble())
                    val predY = spline[1](knownChain.size.toDouble())
                    val predZ = spline[2](knownChain.size.toDouble())
                    if ((predX - x) + (predY - y) + (predZ - z) < MAX_CHAIN_DISTANCE_ERROR) {
                        knownChain.add(part)
                        updateGuess()
                        return@on
                    }
                }
            }

            if (spadeUsePositions.any {
                    t < it.t &&
                    (x - it.x).pow(2) +
                    (y - it.y).pow(2) +
                    (z - it.z).pow(2) < 4
                }) possibleStartingParticles.add(part)
            else unclaimedParticles.add(part)

            ransac()
        }

        on<PacketSentEvent> { event ->
            val hand = when (val packet = event.packet) {
                is PlayerInteractItemC2SPacket -> packet.hand
                is PlayerInteractBlockC2SPacket -> packet.hand
                else -> null
            } ?: return@on
            val itemStack = minecraft.player?.getStackInHand(hand) ?: return@on

            val sbId = ItemUtils.skyblockId(itemStack) ?: return@on
            if (!isSpade(sbId)) return@on

            val player = minecraft.player as ClientPlayerEntityAccessor? ?: return@on

            spadeUsePositions.add(
                PositionTime(
                    EventBus.totalTicks + (Ping.getMedianPing() / 50.0 + 10.0).toInt(),
                    player.lastXClient,
                    player.lastYClient,
                    player.lastZClient
                )
            )
        }

        on<PacketSentEvent> { event ->
            val pos = when (val packet = event.packet) {
                is PlayerActionC2SPacket -> {
                    if (packet.action != PlayerActionC2SPacket.Action.START_DESTROY_BLOCK) return@on
                    packet.pos
                }

                is PlayerInteractBlockC2SPacket -> {
                    val hand = packet.hand
                    val itemStack = minecraft.player?.getStackInHand(hand) ?: return@on

                    val sbId = ItemUtils.skyblockId(itemStack) ?: return@on
                    if (!isSpade(sbId)) return@on
                    packet.blockHitResult.blockPos
                }

                else -> return@on
            }

            BurrowManager.digBurrow(pos)
        }

        on<TickEvent> {
            val itemStack = minecraft.player?.mainHandStack ?: return@on
            val sbId = ItemUtils.skyblockId(itemStack) ?: return@on
            if (!isSpade(sbId)) return@on

            BurrowManager.burrows.removeIf {
                if (it.type.empirical) return@removeIf false
                it.ttl -= 5 * 60
                it.ttl <= 0
            }
        }

        on<RenderWorldEvent> {
            BurrowManager.burrows.forEach {
                if (it.type.empirical) return@forEach

                Context.Immediate?.renderWaypoint(
                    it.x, it.y, it.z,
                    when (it.type) {
                        BurrowManager.BurrowType.GUESS -> SETTING_GUESS_COLOR
                        BurrowManager.BurrowType.OLD_GUESS -> SETTING_OLD_GUESS_COLOR
                        else -> Color(0)
                    },
                    it.type.name,
                    increase = true,
                    phase = true
                )
            }

            val guess = guessPos.value
            if (guess != null) Context.Immediate?.renderWaypoint(
                guess.x, guess.y, guess.z,
                SETTING_GUESS_COLOR,
                BurrowManager.BurrowType.GUESS.name,
                increase = true,
                phase = true
            )

            // TODO: renderLine
            /*
            val path = particlePath.value
            for (i in path.indices step 3) {
                val x = path[i + 0]
                val y = path[i + 1]
                val z = path[i + 2]
            }
            */
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        fullReset()
    }
}