package com.github.synnerz.devonian.features.diana

import com.github.synnerz.barrl.Context
import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.Ping
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.mixin.accessor.LocalPlayerAccessor
import com.github.synnerz.devonian.utils.math.MathUtils
import com.github.synnerz.devonian.utils.math.MathUtils.lerp
import com.mojang.blaze3d.vertex.PoseStack
import kotlinx.atomicfu.atomic
import net.minecraft.client.renderer.RenderType
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.network.protocol.game.ServerboundUseItemPacket
import org.joml.Vector3f
import java.awt.Color
import java.util.*
import kotlin.math.*
import kotlin.random.Random

// Credits to https://github.com/hannibal002/SkyHanni/blob/beta/src/main/java/at/hannibal2/skyhanni/features/event/diana/PreciseGuessBurrow.kt
// & https://github.com/PerseusPotter/chicktils/blob/master/modules/diana.js
object BurrowGuesser : Feature(
    "burrowGuesser",
    "Whenever right clicking on a spade, it will attempt to guess where the location will be a",
    "Diana",
    "hub"
) {
    private val SETTING_GUESS_COLOR = addColorPicker(
        "guessColor",
        "Color of current guess",
        "Guess Color",
        Color.BLUE.rgb
    )
    private val SETTING_OLD_GUESS_COLOR = addColorPicker(
        "oldGuessColor",
        "Color of old guess",
        "Old Guess Color",
        Color(82, 14, 125).rgb
    )
    private val SETTING_REMEMBER_PREVIOUS_GUESSES = addSwitch(
        "storeGuesses",
        "Remember locations of previous guesses",
        "Remember Guesses",
        true
    )
    private val SETTING_PARTICLE_PATH_COLOR = addColorPicker(
        "particlePathColor",
        "Color of path of particles",
        "Particle Path Color",
        0
    )

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
        knownChain.clear()

        if (SETTING_REMEMBER_PREVIOUS_GUESSES.get()) {
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

        guessPos.value = PositionTime(0, poly[0](weightT) - 0.5, poly[1](weightT) - 1.0, poly[2](weightT) - 0.5)
        particlePath.value = DoubleArray(300) {
            val i = it / 3
            val o = it % 3
            val t = MathUtils.rescale(i.toDouble(), 0.0, 99.0, 0.0, weightT)
            poly[o](t)
        }
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
        val t = EventBus.serverTicks()
        possibleStartingParticles.removeIf { it.t < t - 20 }
        unclaimedParticles.removeIf { it.t < t - 20 }

        val L1 = possibleStartingParticles.size
        val L2 = unclaimedParticles.size
        val L = L1 + L2
        if (L < MIN_CHAIN_LENGTH) return
        if (L1 == 0) return

        val comb =
            MathUtils.binomial(L, MIN_CHAIN_LENGTH) -
            (
                if (L - L2 < MIN_CHAIN_LENGTH) 0
                else MathUtils.binomial(L - L2, MIN_CHAIN_LENGTH)
            )
        val rand = IntArray(L - 1) { it }
        var start = 0

        var best = mutableListOf<PositionTime>()
        val tried = mutableSetOf<Int>()

        for (i in 0 until min(comb, RANSAC_ITERS_PER)) {
            shuffleUntil(rand, MIN_CHAIN_LENGTH - 1)
            start = Random.nextInt(0, L1)
            val possInliersI = IntArray(MIN_CHAIN_LENGTH) {
                if (it == 0) start
                else {
                    var idx = rand[it - 1]
                    if (idx >= start) idx++
                    idx
                }
            }
            var hash = 1
            possInliersI.forEach { hash *= MathUtils.FIRST_100_PRIMES[it] }
            if (!tried.add(hash)) continue

            val possInliers = Array(MIN_CHAIN_LENGTH) {
                val idx = possInliersI[it]
                if (idx < L1) possibleStartingParticles[idx] else unclaimedParticles[idx - L1]
            }
            val minT = possInliers.minOf { it.t }
            val time = DoubleArray(MIN_CHAIN_LENGTH) { (possInliers[it].t - minT).toDouble() }
            val cX = MathUtils.polyRegression(3, time, DoubleArray(MIN_CHAIN_LENGTH) { possInliers[it].x }) ?: continue
            val cY = MathUtils.polyRegression(3, time, DoubleArray(MIN_CHAIN_LENGTH) { possInliers[it].y }) ?: continue
            val cZ = MathUtils.polyRegression(3, time, DoubleArray(MIN_CHAIN_LENGTH) { possInliers[it].z }) ?: continue
            val polyX = MathUtils.toPolynomial(cX)
            val polyY = MathUtils.toPolynomial(cY)
            val polyZ = MathUtils.toPolynomial(cZ)

            val inliers = mutableListOf<PositionTime>()
            val addIf: (PositionTime) -> Unit = { v ->
                val px = polyX((v.t - minT).toDouble())
                val py = polyY((v.t - minT).toDouble())
                val pz = polyZ((v.t - minT).toDouble())
                if (
                    (v.x - px).pow(2) +
                    (v.y - py).pow(2) +
                    (v.z - pz).pow(2) < 0.1
                ) inliers.add(v)
            }
            possibleStartingParticles.forEach(addIf)
            unclaimedParticles.forEach(addIf)

            if (inliers.size < MIN_CHAIN_LENGTH) continue

            best = inliers
        }

        if (best.isNotEmpty()) {
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
                packet !is ClientboundLevelParticlesPacket ||
                packet.particle.type != ParticleTypes.DRIPPING_LAVA ||
                packet.count != 2 ||
                packet.maxSpeed != -0.5f ||
                !packet.alwaysShow() ||
                packet.xDist != 0f ||
                packet.yDist != 0f ||
                packet.zDist != 0f
            ) return@on

            val t = EventBus.serverTicks()
            val x = packet.x
            val y = packet.y
            val z = packet.z
            val part = PositionTime(t, x, y, z)
            if (knownChain.isNotEmpty() && t < knownChain.last().t + 5) {
                val spline = splinePoly.value
                if (spline != null) {
                    val predX = spline[0](knownChain.size.toDouble())
                    val predY = spline[1](knownChain.size.toDouble())
                    val predZ = spline[2](knownChain.size.toDouble())
                    if (abs(predX - x) + abs(predY - y) + abs(predZ - z) < MAX_CHAIN_DISTANCE_ERROR) {
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
                is ServerboundUseItemPacket -> packet.hand
                is ServerboundUseItemOnPacket -> packet.hand
                else -> null
            } ?: return@on
            val itemStack = minecraft.player?.getItemInHand(hand) ?: return@on

            val sbId = ItemUtils.skyblockId(itemStack) ?: return@on
            if (!isSpade(sbId)) return@on

            val player = minecraft.player as LocalPlayerAccessor? ?: return@on

            spadeUsePositions.add(
                PositionTime(
                    EventBus.serverTicks() + (Ping.getMedianPing() / 50.0 + 10.0).toInt(),
                    player.lastXClient,
                    player.lastYClient + minecraft.player!!.eyeHeight,
                    player.lastZClient
                )
            )
        }

        on<PacketSentEvent> { event ->
            val (pos, itemStack) = when (val packet = event.packet) {
                is ServerboundPlayerActionPacket -> {
                    if (packet.action != ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) return@on
                    val itemStack = minecraft.player?.mainHandItem ?: return@on

                    Pair(packet.pos, itemStack)
                }

                is ServerboundUseItemOnPacket -> {
                    val hand = packet.hand
                    val itemStack = minecraft.player?.getItemInHand(hand) ?: return@on

                    Pair(packet.hitResult.blockPos, itemStack)
                }

                else -> return@on
            }

            val sbId = ItemUtils.skyblockId(itemStack) ?: return@on
            if (!isSpade(sbId)) return@on

            BurrowManager.digBurrow(pos)
        }

        on<TickEvent> {
            val player = minecraft.player ?: return@on
            val itemStack = player.mainHandItem ?: return@on
            val sbId = ItemUtils.skyblockId(itemStack) ?: return@on
            if (!isSpade(sbId)) return@on

            BurrowManager.burrows.removeIf {
                if (it.type.empirical) return@removeIf false
                if ((it.x - player.x).pow(2) + (it.z - player.z).pow(2) >= 100.0) return@removeIf false
                it.ttl -= 5 * 60
                it.ttl <= 0
            }
        }

        on<RenderWorldEvent> { event ->
            BurrowManager.burrows.forEach {
                if (it.type.empirical) return@forEach

                Context.Immediate?.renderWaypoint(
                    it.x, it.y, it.z,
                    when (it.type) {
                        BurrowManager.BurrowType.GUESS -> SETTING_GUESS_COLOR.getColor()
                        BurrowManager.BurrowType.OLD_GUESS -> SETTING_OLD_GUESS_COLOR.getColor()
                        else -> Color(0, true)
                    },
                    it.type.displayName,
                    increase = true,
                    phase = true
                )
            }

            val guess = guessPos.value
            if (guess != null) Context.Immediate?.renderWaypoint(
                guess.x, guess.y, guess.z,
                SETTING_GUESS_COLOR.getColor(),
                BurrowManager.BurrowType.GUESS.displayName,
                increase = true,
                phase = true
            )

            val consumer = minecraft.renderBuffers().bufferSource().getBuffer(RenderType.LINES)
            val camPos = event.ctx.worldState().cameraRenderState.pos ?: return@on
            val stack = PoseStack()
            stack.pushPose()
            stack.translate(camPos.reverse())

            val path = particlePath.value
            for (i in path.indices step 3) {
                if (i == 0) continue
                val x1 = path[i - 3].toFloat()
                val y1 = path[i - 2].toFloat()
                val z1 = path[i - 1].toFloat()
                val x2 = path[i + 0].toFloat()
                val y2 = path[i + 1].toFloat()
                val z2 = path[i + 2].toFloat()

                val normalized = Vector3f(x2 - x1, y2 - y1, z2 - z1).normalize()

                consumer
                    .addVertex(stack.last(), x1, y1, z1)
                    .setColor(SETTING_PARTICLE_PATH_COLOR.get())
                    .setNormal(stack.last(), normalized)

                consumer
                    .addVertex(stack.last(), x2, y2, z2)
                    .setColor(SETTING_PARTICLE_PATH_COLOR.get())
                    .setNormal(stack.last(), normalized)
            }

            stack.popPose()
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        fullReset()
    }
}