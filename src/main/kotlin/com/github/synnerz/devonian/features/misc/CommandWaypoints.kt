package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.Location
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.api.events.UseItemOnEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.mixin.accessor.ScreenAccessor
import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.PersistentJsonClass
import com.github.synnerz.devonian.utils.render.Render3DImmediate
import com.google.gson.reflect.TypeToken
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.gui.components.ChatComponent
import net.minecraft.core.BlockPos
import org.lwjgl.glfw.GLFW
import java.awt.Color

object CommandWaypoints : Feature(
    "commandWaypoints",
    "Allows you to make waypoints on blocks that you can right click on and it'll send the assigned command /dv cmdw",
    Categories.MISC,
    subcategory = "General",
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Location.stateInSkyblock)
    }

    private val waypointsData = object : PersistentJsonClass<MutableMap<String, MutableList<WaypointCommand>>> (
        "devonian/commandwaypoints.json",
        object : TypeToken<MutableMap</* area */String, MutableList<WaypointCommand>>>() {}
    ) {
        override fun onLoadDefault() {
            data = mutableMapOf()
        }
    }
    private var editMode = false
    private var lastMessage = -1L
    private var cmdPos: BlockPos? = null

    data class WaypointCommand(
        var command: String,
        val x: Int,
        val y: Int,
        val z: Int,
        var colorInt: Int = Color.CYAN.rgb,
        var bgBoxInt: Int = Color(0, true).rgb
    ) {
        val color: Color
            get() = Color(colorInt, true)

        fun matches(bp: BlockPos): Boolean
            = bp.x == x && bp.y == y && bp.z == z

        companion object {
            fun fromBP(command: String, bp: BlockPos): WaypointCommand
                = WaypointCommand(command, bp.x, bp.y, bp.z)
        }
    }

    override fun initialize() {
        waypointsData.load()
        if (waypointsData.data.isNullOrEmpty()) waypointsData.onLoadDefault()

        DevonianCommand.command.subcommand("cmdw", true) { _, args ->
            val area = Location.area
            if (area == null) {
                ChatUtils.sendMessage("&cCMDW you are not in a valid Skyblock Area", true)
                return@subcommand 0
            }

            if (args.firstOrNull() == "setcmd") {
                if (cmdPos == null) {
                    ChatUtils.sendMessage("&cCMDW There is not a previous shift + right click block in edit mode", true)
                    return@subcommand 0
                }
                val cmdw = waypointsData.data!![area]?.find { it.matches(cmdPos!!) }
                if (cmdw == null) {
                    ChatUtils.sendMessage("&cCMDW Could not find the waypoint to set Command at", true)
                    cmdPos = null
                    return@subcommand 0
                }
                val cmd = args.getOrNull(1) as? String?
                if (cmd == null) {
                    ChatUtils.sendMessage("&CMDW There was an issue detecting the command, try again", true)
                    return@subcommand 0
                }

                cmdw.command = cmd
                cmdPos = null
                ChatUtils.sendMessage("&aCMDW Successfully set command &b$cmd", true)

                return@subcommand 1
            }
            editMode = !editMode
            ChatUtils.sendMessage("&aCMDW ${if (editMode) "&aon" else "&coff"} &bedit mode", true)
            if (editMode)
                ChatUtils.sendMessage("&6* &7Right click a block to create a waypoint, shift right click it to set a command to it")
            1
        }
            .word("type")
            .greedyString("args")
            .suggest("type", *listOf("setcmd").toTypedArray())

        on<UseItemOnEvent> { event ->
            if (!event.isMainHand()) return@on
            val area = Location.area ?: return@on

            val bp = event.blockHitResult.blockPos
            val cached = waypointsData.data!![area]?.find { it.matches(bp) }

            if (!editMode) {
                if (cached != null && cached.command.isNotEmpty() && lastMessage == -1L || System.currentTimeMillis() - lastMessage > 500) {
                    lastMessage = System.currentTimeMillis()
                    cached?.command?.let { ChatUtils.command(it) }
                }
                return@on
            }

            if (cached != null) {
                if (InputConstants.isKeyDown(minecraft.window, GLFW.GLFW_KEY_LEFT_SHIFT)) {
                    cmdPos = bp
                    Scheduler.scheduleTask {
                        minecraft.gui.openChatScreen(ChatComponent.ChatMethod.COMMAND)
                        Scheduler.scheduleTask {
                            (minecraft.gui.screen() as ScreenAccessor?)?.insertText("/dv cmdw setcmd ", true)
                        }
                    }
                    return@on
                }
                waypointsData.data!![area]?.remove(cached)
                cmdPos = null
                ChatUtils.sendMessage("&aCMDW &cremoved &awaypoints from $area", true)
                return@on
            }

            if (!waypointsData.data!!.containsKey(area)) waypointsData.data!![area] = mutableListOf()

            waypointsData.data!![area]?.add(WaypointCommand.fromBP("", bp))
            ChatUtils.sendMessage("&aCMDW &badded &awaypoints to $area", true)
        }

        on<RenderWorldEvent> {
            val area = Location.area ?: return@on

            waypointsData.data?.get(area)?.forEach { data ->
                val x = data.x + 0.5
                val y = data.y.toDouble()
                val z = data.z + 0.5
                val color = data.color

                Render3DImmediate.renderFilledBox(
                    x, y, z,
                    1.0, 1.0,
                    Color(color.red, color.green, color.blue, color.alpha / 4),
                    true,
                    centered = true,
                )
                Render3DImmediate.renderWireframeBox(
                    x, y, z,
                    1.0, 1.0,
                    color,
                    1.0,
                    true,
                    centered = true,
                )

                Render3DImmediate.renderString(
                    "/${data.command}",
                    x, y + 0.8, z,
                    maxDist = 1000.0,
                    phase = true,
                )
            }
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        editMode = false
    }
}