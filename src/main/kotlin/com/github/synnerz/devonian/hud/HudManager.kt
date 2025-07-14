package com.github.synnerz.devonian.hud

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.utils.JsonUtils
import com.github.synnerz.devonian.utils.Scheduler
import com.github.synnerz.devonian.utils.render.Render2D
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text

object HudManager : Screen(Text.literal("Devonian.HudManager")) {
    private var selectedHud: Hud? = null
    val huds = mutableListOf<Hud>()

    fun initialize() {
        DevonianCommand.command.subcommand("huds") { _, args ->
            Scheduler.scheduleTask(1) {
                Devonian.minecraft.setScreen(this)
            }
            return@subcommand 1
        }

        JsonUtils.preSave {
            for (hud in huds)
                hud.save()
        }
    }

    override fun init() {
        super.init()

        ScreenMouseEvents.afterMouseScroll(this).register { _, x, y, _, dy ->
            onMouseScroll(dy)
        }
    }

    override fun close() {
        super.close()

        selectedHud = null
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        onMouseClicked(mouseX, mouseY, button)
        return false
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        if (selectedHud != null) {
            selectedHud!!.onMouseDrag(deltaX, deltaY)
        }
        return false
    }

    override fun render(context: DrawContext?, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        super.render(context, mouseX, mouseY, deltaTicks)
        Render2D.drawString(
            context!!,
            "Hud Manager",
            10, 10
        )

        for (hud in huds) hud.sampleDraw(context)
    }

    fun onMouseScroll(delta: Double) {
        if (selectedHud == null) return

        selectedHud!!.onMouseScroll(delta)
    }

    fun onMouseClicked(mx: Double, my: Double, mbtn: Int) {
        if (mbtn != 0) return

        var found = false

        for (hud in huds) {
            if (!hud.inBounds(mx, my)) continue

            selectedHud = hud
            found = true
            break
        }

        if (found) return

        selectedHud = null
    }

    fun createHud(name: String, string: String): Hud {
        val hud = Hud(name, string)
        huds.add(hud)
        return hud
    }
}