package com.github.synnerz.devonian.features.misc

import com.github.synnerz.barrl.Context
import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.events.ChatChannelEvent
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.features.Feature
import java.awt.Color
import kotlin.math.sqrt

object ChatWaypoint : Feature("chatWaypoint") {
    private const val SETTING_ALL_CHAT = true
    private const val SETTING_OWN_WAYPOINTS = true
    private const val SETTING_CLEAR_WAYPOINTS_SWAP_SERVER = true
    private val SETTING_WAYPOINT_COLOR = Color.CYAN
    private const val SETTING_SHOW_SENDER_NAME = true

    private val coordRegex = "(?:\\s|^)(?:x: )?(-?\\d+)(?:, y:)? (-?\\d+)(?:, z:)? (-?\\d+)(?:\\s|\$)".toRegex()
    private val waypoints = mutableListOf<TimedWaypoint>()

    data class TimedWaypoint(val created: Long, val by: String, val x: Double, val y: Double, val z: Double)

    fun tryAddWaypoint(name: String, msg: String) {
        if (!SETTING_OWN_WAYPOINTS) return

        val matches = coordRegex.matchEntire(msg) ?: return
        val (xs, ys, zs) = matches.groupValues.drop(1)
        val x = xs.toDouble()
        val y = ys.toDouble()
        val z = zs.toDouble()

        waypoints.add(TimedWaypoint(System.currentTimeMillis(), name, x, y, z))
        ChatUtils.sendMessage("&aSet waypoint at &b$x, $y, $z", true)
    }

    override fun initialize() {
        on<ChatChannelEvent.AllChatEvent> { event ->
            if (!SETTING_ALL_CHAT) return@on
            tryAddWaypoint(event.name, event.userMessage)
        }
        on<ChatChannelEvent.PartyChatEvent> { event ->
            tryAddWaypoint(event.name, event.userMessage)
        }
        on<ChatChannelEvent.CoopChatEvent> { event ->
            tryAddWaypoint(event.name, event.userMessage)
        }

        on<WorldChangeEvent> { if (SETTING_CLEAR_WAYPOINTS_SWAP_SERVER) waypoints.clear() }

        on<RenderWorldEvent> {
            waypoints.removeIf {
                val pos = minecraft.player ?: return@removeIf false
                val dx = it.x - pos.x
                val dy = it.y + 5 - pos.y
                val dz = it.z - pos.z
                val distance = sqrt(dx * dx + dy * dy + dz * dz)
                if (distance < 5) return@removeIf true

                Context.Immediate?.renderWaypoint(
                    it.x,
                    it.y,
                    it.z,
                    SETTING_WAYPOINT_COLOR,
                    title = "%.2fm".format(distance),
                    increase = true,
                    phase = true
                )
                if (SETTING_SHOW_SENDER_NAME) Context.Immediate?.renderString(
                    it.by,
                    it.x,
                    it.y,
                    it.z,
                    backgroundBox = true,
                    increase = true,
                    phase = true
                )
                System.currentTimeMillis() - it.created > 60000
            }
        }
    }
}