package com.github.synnerz.devonian.features.debug

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.config.json.JsonDataObject
import com.github.synnerz.devonian.features.misc.inventory.NoCursorReset
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.DebugLogger

object MousePositionLogger : TextHudFeature(
    "mousePositionLogger",
    "",
    Categories.DEBUG,
    subcategory = "Utils",
) {
    private val SETTING_START = addButton(
        ::startLogger,
        displayName = "Start Logger",
    )
    private val SETTING_STOP = addButton(
        ::stopLogger,
        displayName = "Stop Logger",
    )

    private val mouseLogger = DebugLogger("MouseLogger")

    fun startLogger() {
        if (!isEnabled()) return
        if (mouseLogger.startLogger()) ChatUtils.sendMessage("§aMouse Logger started")
        else ChatUtils.sendMessage("§4Mouse Logger already active")
    }

    fun stopLogger() {
        if (!isEnabled()) return
        mouseLogger.stopAndPrint()
    }

    fun onMove(l: Long, x: Double, y: Double, first: Boolean, x1: Double, y1: Double) {
        if (!isEnabled()) return

        val o = JsonDataObject()
        o.set("type", "move")
        o.set("handle", l)
        o.set("x", x)
        o.set("y", y)
        o.set("time", System.currentTimeMillis() - mouseLogger.startTime)
        o.set("ignore", NoCursorReset.ignoreFirstBatch)
        o.set("first", first)
        o.set("x1", x1)
        o.set("y1", y1)

        mouseLogger.offer(o)
    }

    fun onGrab() {
        if (!isEnabled()) return

        val o = JsonDataObject()
        o.set("type", "grab")
        o.set("x", minecraft.mouseHandler.xpos())
        o.set("y", minecraft.mouseHandler.ypos())
        o.set("time", System.currentTimeMillis() - mouseLogger.startTime)

        mouseLogger.offer(o)
    }

    fun onRelease() {
        if (!isEnabled()) return

        val o = JsonDataObject()
        o.set("type", "release")
        o.set("x", minecraft.mouseHandler.xpos())
        o.set("y", minecraft.mouseHandler.ypos())
        o.set("time", System.currentTimeMillis() - mouseLogger.startTime)

        mouseLogger.offer(o)
    }

    override fun initialize() {
        on<RenderOverlayEvent> { event ->
            if (mouseLogger.startTime == 0L) return@on
            setLines(
                listOf(
                    "${mouseLogger.logFile?.name}",
                    "Time: ${System.currentTimeMillis() - mouseLogger.startTime}",
                )
            )
            draw(event.ctx)
        }.setEnabled(mouseLogger.loggerEnabled)
    }

    override fun getEditText(): List<String> = listOf(
        "devonian-MouseLogger-1770011529466.log",
        "Time: 6942",
    )
}