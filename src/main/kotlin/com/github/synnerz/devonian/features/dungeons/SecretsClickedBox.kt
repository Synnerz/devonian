package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.barrl.Context
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.dungeon.DungeonEvent
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.features.Feature
import net.minecraft.core.BlockPos
import java.awt.Color

object SecretsClickedBox : Feature(
    "secretsClickedBox",
    "Highlights the secrets you have clicked surrounding them with a box, if a chest secret for example is locked the color will change to red.",
    "Dungeons",
    "catacombs"
) {
    private val lockedChestRegex = "^That chest is locked!$".toRegex()
    private val SETTING_BLOCK_COLOR = addColorPicker(
        "blockColor",
        "",
        "Clicked Block Color",
        Color(0, 255, 255, 50).rgb
    )
    private val SETTING_LOCKED_BLOCK_COLOR = addColorPicker(
        "lockedBlockColor",
        "",
        "Locked Block Color",
        Color(255, 0, 0, 50).rgb
    )
    var clickedBlock: BlockPos? = null
    var wasLocked = false

    override fun initialize() {
        on<DungeonEvent.SecretClicked> {
            clickedBlock = BlockPos(it.x.toInt(), it.y.toInt(), it.z.toInt())
            val prevBlock = clickedBlock
            Scheduler.scheduleTask(20) {
                if (clickedBlock != prevBlock) return@scheduleTask
                clickedBlock = null
                wasLocked = false
            }
        }

        on<ChatEvent> { event ->
            event.matches(lockedChestRegex) ?: return@on
            wasLocked = true
        }

        on<RenderWorldEvent> {
            if (clickedBlock == null) return@on
            val immediate = Context.Immediate ?: return@on
            immediate.renderFilledBox(
                clickedBlock!!.x.toDouble(), clickedBlock!!.y.toDouble(), clickedBlock!!.z.toDouble(),
                if (wasLocked) SETTING_LOCKED_BLOCK_COLOR.getColor() else SETTING_BLOCK_COLOR.getColor(), true
            )

            immediate.renderBox(
                clickedBlock!!.x.toDouble(), clickedBlock!!.y.toDouble(), clickedBlock!!.z.toDouble(),
                if (wasLocked) Color.RED else Color.CYAN, true
            )
        }

        on<WorldChangeEvent> {
            clickedBlock = null
            wasLocked = false
        }
    }
}