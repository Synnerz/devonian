package com.github.synnerz.devonian.features.misc

import com.github.synnerz.barrl.Context
import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.features.Feature
import kotlin.math.sqrt

object ChatWaypoint : Feature("chatWaypoint") {
    private val chatRegex = "^(Co-op|Party)?(?: > )?(?:\\[\\d+] .? ?)?(?:\\[[^]]+] )?(\\w{1,16}): x: (-?\\d+), y: (-?\\d+), z: (-?\\d+) ?$".toRegex()
    private val waypoints = mutableListOf<TimedWaypoint>()

    data class TimedWaypoint(val created: Long, val by: String, val x: Double, val y: Double, val z: Double)

    override fun initialize() {
        on<ChatEvent> { event ->
            val matches = event.matches(chatRegex) ?: return@on
            val ( type, username, x1, y1, z1 ) = matches
            if (type.isEmpty()) return@on
            val x = x1.toDouble()
            val y = y1.toDouble()
            val z = z1.toDouble()

            waypoints.add(TimedWaypoint(System.currentTimeMillis(), username, x, y, z))
            ChatUtils.sendMessage("&aSet waypoint at &b$x, $y, $z", true)
        }

        on<WorldChangeEvent> { waypoints.clear() }

        on<RenderWorldEvent> {
            waypoints.removeIf {
                val pos = Devonian.minecraft.player ?: return@removeIf false
                val dx = it.x - pos.x
                val dy = it.y + 5 - pos.y
                val dz = it.z - pos.z
                val distance = sqrt(dx * dx + dy * dy + dz * dz)
                if (distance < 5) return@removeIf true

                Context.Immediate?.renderWaypoint(it.x, it.y, it.z, title = "%.2fm".format(distance), increase = true, phase = true)
                System.currentTimeMillis() - it.created > 60000
            }
        }
    }
}