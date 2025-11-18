package com.github.synnerz.devonian.config.ui

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.hud.HudManager
import com.github.synnerz.talium.components.*
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

object ConfigGui : Screen(Component.literal("Devonian.ConfigGui")) {
    private val background = UIRect(0.0, 0.0, 100.0, 100.0)
    private val main = UIRect(20.0, 15.0, 60.0, 60.0, parent = background).apply {
        setColor(ColorPalette.PRIMARY_COLOR)
    }
    private val leftPanel = UIRect(0.0, 0.0, 20.0, 100.0, parent = main).apply {
        setColor(ColorPalette.SECONDARY_COLOR)
    }
    private val rightPanel = UIRect(21.0, 1.0, 78.5, 98.0, parent = main).apply {
        setColor(ColorPalette.SECONDARY_COLOR)
    }
    private val configTitle = UIText(0.0, 0.0, 100.0, 10.0,"Devonian", true, leftPanel).apply {
        setColor(ColorPalette.TITLE_COLOR)
    }
    private val editHudsButton = UIRect(32.5, 90.0, 35.0, 8.0, parent = rightPanel).apply {
        setColor(ColorPalette.TERTIARY_COLOR)
        addChild(UIText(0.0, 0.0, 100.0, 100.0, "Edit Huds", true).apply {
            setColor(ColorPalette.TEXT_COLOR)
        })
        onMouseRelease {
            if (it.button != 0) return@onMouseRelease
            Scheduler.scheduleTask(1) {
                Devonian.minecraft.setScreen(HudManager)
            }
        }
    }
    val categories = mutableListOf<Category>()
    var selectedCategory: Category

    init {
        createCategories()
        selectedCategory = categories.first()
        selectedCategory.unhide()
    }

    private fun createCategories() {
        // All our categories should be created here
        categories.add(Category("Dungeons", rightPanel, leftPanel))
        categories.add(Category("Garden", rightPanel, leftPanel))
        categories.add(Category("Slayers", rightPanel, leftPanel))
        categories.add(Category("End", rightPanel, leftPanel))
        categories.add(Category("Diana", rightPanel, leftPanel))
        categories.add(Category("Misc", rightPanel, leftPanel))
    }

    fun category(name: String): Category {
        return categories.find { it.categoryName.lowercase() == name.lowercase() }!!
    }

    fun initialize() {
        selectedCategory.update()
        DevonianCommand.command.subcommand("configui") { _, args ->
            Scheduler.scheduleTask(1) {
                Devonian.minecraft.setScreen(this)
            }
            return@subcommand 1
        }
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        super.render(context, mouseX, mouseY, deltaTicks)
        background.draw()
    }

    override fun renderBackground(context: GuiGraphics, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        // no background here bud
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        background.handleKeyInput(keyCode, scanCode)
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun shouldCloseOnEsc(): Boolean {
        return false
    }
}