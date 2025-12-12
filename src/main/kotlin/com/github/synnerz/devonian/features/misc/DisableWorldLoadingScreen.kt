package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.events.GuiOpenEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.mixin.accessor.LevelLoadingScreenAccessor
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.LevelLoadingScreen
import net.minecraft.client.gui.screens.Overlay

object DisableWorldLoadingScreen : Feature("disableWorldLoadingScreen") {
    private var levelLoadingScreen: LevelLoadingScreenAccessor? = null

    override fun initialize() {
        on<GuiOpenEvent> { event ->
            if (event.screen !is LevelLoadingScreen) {
                if (minecraft.overlay is PausingOverlay) minecraft.overlay = null
                return@on
            }
            levelLoadingScreen = event.screen as? LevelLoadingScreenAccessor
            event.cancel()
            minecraft.setScreen(null)
            minecraft.overlay = PausingOverlay
            // minecraft.noRender = true
        }
    }

    fun onPlayerLoaded() {
        minecraft.overlay = null
        // minecraft.noRender = false
    }

    object PausingOverlay : Overlay() {
        override fun render(guiGraphics: GuiGraphics, i: Int, j: Int, f: Float) {}

        override fun tick() {
            val screen = levelLoadingScreen ?: return
            if (screen.loadTracker.isLevelReady || (minecraft.singleplayerServer?.isReady ?: false)) minecraft.overlay = null
        }
    }
}