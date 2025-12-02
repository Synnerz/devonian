package com.github.synnerz.devonian.features.diana

import com.github.synnerz.devonian.api.Ping
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.api.events.TickEvent
import net.minecraft.core.BlockPos
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.round

object BurrowManager {
    val burrows = CopyOnWriteArrayList<Burrow>()
    private val recentDugBurrows = LinkedList<DugBurrow>()

    data class Burrow(val type: BurrowType, val x: Double, val y: Double, val z: Double, var ttl: Int) {
        fun sameBlockPos(bp: BlockPos): Boolean {
            return bp.x == round(x).toInt() && bp.y == round(y).toInt() && bp.z == round(z).toInt()
        }

        fun sameBlockPos(x: Double, y: Double, z: Double) =
            floor(this.x) == floor(x) &&
            floor(this.y) == floor(y) &&
            floor(this.z) == floor(z)
    }

    enum class BurrowType(val displayName: String, val empirical: Boolean) {
        START("§aStart", true),
        MOB("§cMob", true),
        TREASURE("§eTreasure", true),
        GUESS("§3Guess", false),
        OLD_GUESS("§5Guess", false),
    }

    data class DugBurrow(val t: Long, val x: Int, val y: Int, val z: Int)

    fun addBurrow(type: BurrowType, x: Double, y: Double, z: Double, ttl: Int = 5 * 60 * 20) {
        val t = System.currentTimeMillis()
        if (burrows.any {
            it.type == type && it.sameBlockPos(x, y, z) ||
            recentDugBurrows.any {
                it.t > t &&
                it.x == round(x).toInt() &&
                it.y == round(y).toInt() &&
                it.z == round(z).toInt()
            }
        }) return

        burrows.add(Burrow(type, x, y, z, ttl))

        if (type.empirical) burrows.removeIf {
            !it.type.empirical &&
            (it.x - x).pow(2) + (it.y - y).pow(2) + (it.z - z).pow(2) < 100
        }
    }

    fun digBurrow(pos: BlockPos) {
        burrows.removeIf { it.sameBlockPos(pos) }
        recentDugBurrows.add(DugBurrow(System.currentTimeMillis() + Ping.getMedianPing().toLong() + 1_000L, pos.x, pos.y, pos.z))
        if (recentDugBurrows.size > 10) recentDugBurrows.remove()
    }

    init {
        EventBus.on<ChatEvent> { event ->
            if (event.message != "Poof! You have cleared your griffin burrows!") return@on
            BurrowGuesser.fullReset()
            burrows.clear()
        }

        EventBus.on<TickEvent> {
            burrows.removeIf { --it.ttl <= 0 }
        }
    }
}