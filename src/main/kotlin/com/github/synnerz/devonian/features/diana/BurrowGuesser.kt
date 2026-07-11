package com.github.synnerz.devonian.features.diana

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.Ping
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.mixin.accessor.LocalPlayerAccessor
import com.github.synnerz.devonian.utils.math.MathUtils
import com.github.synnerz.devonian.utils.render.Render3DImmediate
import kotlinx.atomicfu.atomic
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.client.KeyMapping
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.network.protocol.game.ServerboundUseItemPacket
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import org.lwjgl.glfw.GLFW
import java.awt.Color
import java.util.*
import kotlin.math.*
import kotlin.random.Random

// Credits to https://github.com/hannibal002/SkyHanni/blob/beta/src/main/java/at/hannibal2/skyhanni/features/event/diana/PreciseGuessBurrow.kt
// & https://github.com/PerseusPotter/chicktils/blob/master/modules/diana.js
object BurrowGuesser : Feature(
    "burrowGuesser",
    "Whenever right clicking on a spade, it will attempt to guess where the location will be at.",
    Categories.DIANA,
    "hub",
    searchTags = setOf("diana", "spade"),
) {
    private val SETTING_GUESS_COLOR = addColorPicker(
        "guessColor",
        Color.BLUE.rgb,
        "Color of current guess.",
        "Guess Color",
    )
    private val SETTING_RENDER_BEAM = addSwitch(
        "renderBeam",
        true,
        "Whether to render the beacon beam.",
        "Render Beam"
    )
    private val SETTING_REMEMBER_PREVIOUS_GUESSES = addSwitch(
        "storeGuesses",
        true,
        "Remember locations of previous guesses.",
        "Remember Guesses",
    )
    private val SETTING_OLD_GUESS_COLOR = addColorPicker(
        "oldGuessColor",
        Color(82, 14, 125).rgb,
        "Color of previous guesses.",
        "Old Guess Color",
    )
    private val SETTING_PARTICLE_PATH_COLOR = addColorPicker(
        "particlePathColor",
        0,
        "Color of path of particles.",
        "Particle Path Color",
    )

    private val keybind = KeyMappingHelper.registerKeyMapping(KeyMapping(
        "key.devonian.burrowWarp",
        GLFW.GLFW_KEY_UNKNOWN,
        Devonian.keybindCategory
    ))
    private val warpCommands = listOf(
        WarpCommand("warp stonks", -36.500, 70.0, -81.500),
        WarpCommand("warp castle", -250.0, 130.0, 45.0),
        WarpCommand("warp wizard", 44.500, 119.0, 93.500),
        WarpCommand("warp crypts", -160.500, 62.0, -106.500),
        WarpCommand("warp hub", 0.500, 77.0, -0.500),
        WarpCommand("warp da", 92.500, 75.500, 174.500),
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

    data class PositionTime(val t: Int, val x: Double, val y: Double, val z: Double, var blacklisted: Boolean = false)
    data class WarpCommand(val command: String, val x: Double, val y: Double, val z: Double, var blacklisted: Boolean = false)

    fun resetGuess() {
        splinePoly.value = null
        particlePath.value = doubleArrayOf()
        knownChain.clear()

        if (SETTING_REMEMBER_PREVIOUS_GUESSES.get()) {
            val guess = guessPos.value
            val player = minecraft.player ?: return
            if (
                guess != null &&
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
        guessPos.value?.let {
            // world bounds are around -200~+200
            if (abs(it.x) > 300 || it.y !in 0.0..150.0 || abs(it.z) > 300) {
                Scheduler.scheduleTask {
                    minecraft.level?.playLocalSound(
                        it.x, it.y, it.z,
                        SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS,
                        1f, 1f, false
                    )
                }
                guessPos.value = null
            }
        }
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

        var bestD = Double.POSITIVE_INFINITY
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
                    (v.z - pz).pow(2) < 0.1
                ) inliers.add(v)
            }
            possibleStartingParticles.forEach(addIf)
            unclaimedParticles.forEach(addIf)

            if (inliers.size < MIN_CHAIN_LENGTH) continue

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

            val player = minecraft.player as? LocalPlayerAccessor? ?: return@on

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
            val bp = guessPos.value ?: return@on
            val ( _, bpx, bpy, bpz ) = bp
            val rx = bpx.roundToInt()
            val rz = bpz.roundToInt()

            if (
                pos.x in rx - 1..rx + 1 &&
                pos.y == bpy.roundToInt() &&
                pos.z in rz - 1..rz + 1
            )
                guessPos.value = null
        }

        on<TickEvent> {
            val player = minecraft.player ?: return@on
            val itemStack = player.mainHandItem
            val sbId = ItemUtils.skyblockId(itemStack) ?: return@on
            if (!isSpade(sbId)) return@on

            BurrowManager.burrows.removeIf {
                if (it.type.empirical) return@removeIf false
                if ((it.x - player.x).pow(2) + (it.z - player.z).pow(2) >= 100.0) return@removeIf false
                it.ttl -= 5 * 60
                it.ttl <= 0
            }
        }

        on<KeyPressEvent> { event ->
            // "You haven't unlocked this fast travel destination!"
            if (!keybind.matches(event.underlying)) return@on
            val player = minecraft.player ?: return@on
            val pos = guessPos.value ?: return@on
            val match = warpCommands.toMutableList().also {
                it.add(WarpCommand("", player.x, player.y, player.z))
            }.minByOrNull {
                (pos.x - it.x).pow(2) +
                (pos.y - it.y).pow(2) +
                (pos.z - it.z).pow(2)
            } ?: return@on
            if (match.command.isEmpty()) return@on

            ChatUtils.sendMessage("&aUsing warp command &6${match.command}", true)
            ChatUtils.command(match.command)
        }

        on<RenderWorldEvent> {
            BurrowManager.burrows.forEach {
                guessPos.value?.let { bp ->
                    val ( _, bpx, bpy, bpz ) = bp
                    if (bp.blacklisted) return@let
                    val rx = bpx.roundToInt()
                    val rz = bpz.roundToInt()
                    bp.blacklisted =
                        it.x.roundToInt() in rx - 1..rx + 1 &&
                                it.y.roundToInt() == bpy.roundToInt() &&
                                it.z.roundToInt() in rz - 1..rz + 1
                }

                if (it.type.empirical) return@forEach

                Render3DImmediate.renderWaypoint(
                    it.x, it.y, it.z,
                    when (it.type) {
                        BurrowManager.BurrowType.GUESS -> SETTING_GUESS_COLOR.getColor()
                        BurrowManager.BurrowType.OLD_GUESS -> SETTING_OLD_GUESS_COLOR.getColor()
                        else -> Color(0, true)
                    },
                    phase = true,
                    title = it.type.displayName,
                    textScale = 2f, textMaxDist = 20.0, textBackgroundBox = Color.BLACK,
                    beacon = SETTING_RENDER_BEAM.get(),
                    centered = false,
                )
            }

            val guess = guessPos.value
            if (guess != null) Render3DImmediate.renderWaypoint(
                guess.x, guess.y, guess.z,
                SETTING_GUESS_COLOR.getColor(),
                phase = true,
                title = BurrowManager.BurrowType.GUESS.displayName,
                textScale = 2f, textMaxDist = 20.0, textBackgroundBox = Color.BLACK,
                beacon = SETTING_RENDER_BEAM.get() && !guess.blacklisted,
                centered = true,
            )

            val color = SETTING_PARTICLE_PATH_COLOR.getColor()
            Render3DImmediate.renderLineStrip(color.alpha == 255, phase = true) {
                val path = particlePath.value
                for (i in path.indices step 3) {
                    submit(path[i + 0], path[i + 1], path[i + 2], color)
                }
            }
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        fullReset()
    }
}