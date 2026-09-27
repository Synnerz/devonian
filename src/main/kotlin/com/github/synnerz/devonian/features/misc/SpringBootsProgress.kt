package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.events.ClientThreadServerTickEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.SoundPlayEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import net.minecraft.world.entity.EquipmentSlot
import kotlin.math.roundToInt

object SpringBootsProgress : TextHudFeature(
    "springBootsProgress",
    "Displays the progress amount"
) {
    private val SETTING_MAX_PROGRESS = addSlider(
        "maxProgress",
        10.0,
        0.0, 30.0,
        "Sets the max progress for the display (10 is default)",
        "Max Progress",
    )
    private val validPitches = listOf(0.6984127f, 0.82539684f, 0.8888889f)
    private var progress = 0

    override fun initialize() {
        on<SoundPlayEvent> { event ->
            if (
                event.sound == "minecraft:entity.firework_rocket.launch" && event.pitch == 1.6984127f ||
                event.sound == "minecraft:entity.generic.eat" && event.pitch == 0.0952381f
            ) {
                progress = 0
                return@on
            }
            if (event.sound != "minecraft:block.note_block.pling") return@on
            val player = minecraft.player ?: return@on
            if (!player.isShiftKeyDown) return@on
            val itemStack = player.getItemBySlot(EquipmentSlot.FEET)
            val sbId = ItemUtils.skyblockId(itemStack) ?: return@on
            if (sbId != "SPRING_BOOTS") return@on
            if (event.pitch !in validPitches) return@on

            progress++
        }

        on<ClientThreadServerTickEvent> {
            val player = minecraft.player ?: return@on
            val itemStack = player.getItemBySlot(EquipmentSlot.FEET)
            val sbId = ItemUtils.skyblockId(itemStack)
            if (sbId != "SPRING_BOOTS") {
                clearLines()
                return@on
            }
            val format = if (progress >= SETTING_MAX_PROGRESS.get().roundToInt()) "&a" else if (progress > 5) "&e" else "&c"

            setLine("${format}${progress * 10}%")
        }

        on<RenderOverlayEvent> {
            draw(it.ctx)
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        progress = 0
        clearLines()
    }

    override fun getEditText(): List<String> = listOf("&a100%")
}