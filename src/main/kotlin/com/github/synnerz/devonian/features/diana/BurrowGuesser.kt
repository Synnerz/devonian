package com.github.synnerz.devonian.features.diana

import com.github.synnerz.barrl.Context
import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.features.WorldFeature
import com.github.synnerz.devonian.features.diana.guesser.LorenzVec
import com.github.synnerz.devonian.features.diana.guesser.ParticlePathBezierFitter
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket
import net.minecraft.particle.ParticleTypes
import java.awt.Color
import kotlin.math.abs

// Credits to https://github.com/hannibal002/SkyHanni/blob/beta/src/main/java/at/hannibal2/skyhanni/features/event/diana/PreciseGuessBurrow.kt
// & https://github.com/PerseusPotter/chicktils/blob/master/modules/diana.js

// TODO: whenever there is more time for this feature, remove all the useless (for us) methods
//  inside the helper classes, as well as removing [LorenzVec] since its only use is here
object BurrowGuesser : WorldFeature("burrowGuesser", "hub") {
    private val bezierFitter = ParticlePathBezierFitter(3)
    private var lastDianaSpade = 0L
    private var lastLavaParticle = 0L
    private var currentGuess: LorenzVec? = null

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

            lastLavaParticle = System.currentTimeMillis()
            if (System.currentTimeMillis() - lastDianaSpade > 3000) return@on
            val currPos = LorenzVec(packet.x, packet.y, packet.z)
            if (bezierFitter.isEmpty())
                return@on bezierFitter.addPoint(currPos)

            val distToLast = bezierFitter.getLastPoint()?.distance(currPos) ?: return@on
            if (distToLast == 0.0 || distToLast > 3.0) return@on

            bezierFitter.addPoint(currPos)
            val guessPos = bezierFitter.solve() ?: return@on
            currentGuess = guessPos.roundToBlock()
        }

        on<PacketSentEvent> { event ->
            val hand = when (val packet = event.packet) {
                is PlayerInteractItemC2SPacket -> packet.hand
                is PlayerInteractBlockC2SPacket -> packet.hand
                else -> null
            } ?: return@on
            val itemStack = minecraft.player?.getStackInHand(hand) ?: return@on
            val sbId = ItemUtils.skyblockId(itemStack) ?: return@on

            if (sbId == "ANCESTRAL_SPADE" || sbId == "ARCHAIC_SPADE" || sbId == "DEIFIC_SPADE") {
                if (System.currentTimeMillis() - lastLavaParticle < 200) return@on

                currentGuess = null
                bezierFitter.reset()
                lastDianaSpade = System.currentTimeMillis()
            }
        }

        on<PacketSentEvent> { event ->
            val packet = event.packet
            if (packet !is PlayerActionC2SPacket) return@on
            val action = packet.action
            if (action != PlayerActionC2SPacket.Action.START_DESTROY_BLOCK) return@on
            val pos = packet.pos

            if (currentGuess == null) return@on
            if (
                pos.x == currentGuess!!.x.toInt() &&
                pos.y == currentGuess!!.y.toInt() - 1 &&
                pos.z == currentGuess!!.z.toInt()
            )
                currentGuess = null
        }

        on<RenderWorldEvent> {
            if (currentGuess == null) return@on

            val pos = minecraft.player ?: return@on
            // Manhattan distance 2d
            val distance = abs(pos.x - currentGuess!!.x) + abs(pos.z - currentGuess!!.z)
            if (distance < 6) {
                currentGuess = null
                return@on
            }

            Context.Immediate?.renderWaypoint(
                currentGuess!!.x,
                currentGuess!!.y - 1.0,
                currentGuess!!.z,
                Color.orange,
                "§6§lBurrow Guess",
                phase = true,
                increase = true
            )
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        bezierFitter.reset()
        currentGuess = null
    }
}