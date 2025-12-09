package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.events.ActionbarEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.mixin.accessor.GuiAccessor
import com.github.synnerz.devonian.mixin.accessor.HeartTypeAccessor
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.Gui.HeartType
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.entity.player.Player
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min


object AccurateAbsorption : Feature("accurateAbsorption") {
    private val SETTING_MAX_ABSORPTION_HEARTS = addSlider(
        "maxAbsorptionHearts",
        40.0,
        0.0, 100.0,
        "1 = half a heart, for things like mastiff wish",
        "Max Absorption Hearts",
    )

    private val healthRegex = "\\b([\\d,.]+)/([\\d,.]+)❤".toRegex()

    private var actualAbsorption = 0

    fun renderHearts(
        gui: Gui,
        guiGraphics: GuiGraphics, player: Player,
        left: Int, top: Int,
        rowGap: Int, regen: Int,
        maxHearts: Float, hearts: Int, displayHearts: Int, absorption: Int,
        blinking: Boolean
    ) {
        val gui = gui as? GuiAccessor ?: return
        val hardcore = player.level().levelData.isHardcore
        val heart = HeartTypeAccessor.invokeForPlayer(player)

        val maxHearts = maxHearts.toInt()
        val initialHearts = hearts
        var hearts = hearts
        var absorption = actualAbsorption
        var actualHearts = displayHearts

        val slots = (max(maxHearts, hearts + absorption) + 1) / 2
        for (slot in 0 until slots) {
            val row = slot / 10
            val col = slot % 10
            val x = left + col * 8
            var y = top - row * rowGap
            if (initialHearts <= 4) y += gui.getRandom().nextInt(2)
            if (slot < maxHearts && slot == regen) y -= 2

            gui.invokeRenderHeart(
                guiGraphics, HeartType.CONTAINER,
                x, y,
                hardcore, blinking, false
            )

            val heartHp = min(2, hearts)
            val blink = blinking && actualHearts <= 0

            if (absorption > 0 && heartHp != 2) {
                val absorbHp = min(2 - heartHp, absorption)
                gui.invokeRenderHeart(
                    guiGraphics, HeartType.ABSORBING,
                    x, y,
                    hardcore, false, absorbHp == 1 && heartHp == 0
                )
                absorption -= absorbHp
            }

            if (heartHp > 0) {
                gui.invokeRenderHeart(
                    guiGraphics, heart,
                    x, y,
                    hardcore, blink, heartHp == 1
                )
                hearts -= heartHp
                actualHearts -= heartHp
            }
        }
    }

    override fun initialize() {
        on<ActionbarEvent> { event ->
            val match = healthRegex.find(event.message) ?: return@on
            val (curHp, maxHp) = match.groupValues.drop(1)
            val curHpN = curHp.replace(",", "").toDoubleOrNull() ?: return@on
            val maxHpN = maxHp.replace(",", "").toDoubleOrNull() ?: return@on

            val player = minecraft.player ?: return@on
            val hearts = ceil(player.health).toInt()
            val maxHearts = ceil(player.maxHealth).toInt()
            val hpPerHeart = maxHpN / maxHearts
            actualAbsorption = min(
                max(ceil(curHpN / hpPerHeart).toInt() - hearts, 0),
                SETTING_MAX_ABSORPTION_HEARTS.get().toInt()
            )
        }
    }
}