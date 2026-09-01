package com.github.synnerz.devonian.features.misc.hiders

import com.github.synnerz.devonian.api.Location
import com.github.synnerz.devonian.api.events.GuiOpenEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.mixin.accessor.LevelLoadingScreenAccessor
import com.github.synnerz.devonian.utils.BasicState
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.LevelLoadingScreen
import net.minecraft.client.gui.screens.Overlay

object DisableWorldLoadingScreen : Feature(
    "disableWorldLoadingScreen",
    category = Categories.VANILLA_TWEAKS,
    subcategory = "Hider"
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Location.stateInSkyblock)
    }

    private var levelLoadingScreen: LevelLoadingScreenAccessor? = null

    override fun initialize() {
        on<GuiOpenEvent> { event ->
            if (event.screen !is LevelLoadingScreen) {
                if (minecraft.overlay is PausingOverlay) minecraft.overlay = null
                return@on
            }
            if (Location.area == null) return@on
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
        override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {}

        override fun tick() {
            val screen = levelLoadingScreen ?: return
            if (screen.loadTracker.isLevelReady || (minecraft.singleplayerServer?.isReady ?: false)) minecraft.overlay = null
        }
    }
}