package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.events.ClientThreadServerTickEvent
import com.github.synnerz.devonian.api.events.TickEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.math.MathUtils
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource

object LastBreathPullSound : Feature(
    "lastBreathPullSound",
    "like spring boots",
    Categories.DUNGEONS,
    subcategory = "QOL",
) {
    private val SETTING_VOLUME = addDecimalSlider(
        "volume",
        1.0,
        0.0, 5.0,
        "",
        "Last Breath Volume",
    )
    private val SETTING_THRESHOLD = addSlider(
        "threshold",
        9.0,
        0.0, 21.0,
        "After how many ticks to swap to a different sound. 0: always, 21: never",
        "Last Breath Sound Threshold",
    )

    private val SOUND_CHARGE = SoundEvents.NOTE_BLOCK_PLING
    private val SOUND_THRESH = SoundEvents.ARROW_HIT_PLAYER

    private var pulling = false
    private var ticks = 0

    override fun initialize() {
        on<ClientThreadServerTickEvent> {
            if (!pulling) return@on

            ticks++
            minecraft.level?.playPlayerSound(
                if (ticks >= SETTING_THRESHOLD.get()) SOUND_THRESH
                else SOUND_CHARGE.value(),
                SoundSource.MASTER,
                SETTING_VOLUME.get().toFloat(),
                MathUtils.rescale(ticks.toDouble(), 0.0, 20.0, 0.5, 2.0).toFloat(),
            )
        }

        on<TickEvent> {
            pulling = let {
                val held = minecraft.player?.mainHandItem ?: return@let false
                if (held.isEmpty) return@let false

                if (held !== minecraft.player?.useItem) return@let false

                val id = ItemUtils.skyblockId(held) ?: return@let false
                return@let id == "LAST_BREATH" || id == "STARRED_LAST_BREATH"
            }

            if (!pulling) ticks = 0
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        pulling = false
        ticks = 0
    }
}