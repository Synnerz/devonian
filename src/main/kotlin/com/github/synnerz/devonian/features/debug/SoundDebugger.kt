package com.github.synnerz.devonian.features.debug

import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.config.ConfigData
import com.github.synnerz.devonian.utils.CustomSounds

object SoundDebugger {
    // TODO: add actual sound debugging and not only "play sound"
    val SETTING_SOUND_INPUT = ConfigData.TextInput(
        "soundDebugger\$input",
        "",
        null,
        "input a minecraft sound ex: \"minecraft:item.flintandsteel.use\"",
        "SoundDebugger Input",
        "Misc"
    ).also {
        Config.registerCategory(it, Categories.DEBUG, "Misc")
    }
    val SETTING_SOUND_VOLUME = ConfigData.DecimalSlider(
        "soundDebugger\$volume",
        1.0,
        null,
        0.0, 10.0,
        "the volume of the sound (1 = 100% yes it can go over 100%)",
        "SoundDebugger Volume",
        "Misc"
    ).also {
        Config.registerCategory(it, Categories.DEBUG, "Misc")
    }
    val SETTING_SOUND_PITCH = ConfigData.TextInput(
        "soundDebugger\$pitch",
        "1",
        null,
        "the pitch of the sound",
        "SoundDebugger Pitch",
        "Misc"
    ).also {
        Config.registerCategory(it, Categories.DEBUG, "Misc")
    }
    val customSound = CustomSounds.createFake("fakesound", "minecraft:block.anvil.place")
    val SETTING_SOUND_DEBUG = ConfigData.Button(
        {
            customSound.setValue(SETTING_SOUND_INPUT.get())
            customSound.setVolume(SETTING_SOUND_VOLUME.get().toFloat())
            customSound.setPitch(SETTING_SOUND_PITCH.get().toFloat())
            println("attempting to play sound \"${SETTING_SOUND_INPUT.get()}\" $customSound ||| ${customSound.soundEvent}")

            Scheduler.scheduleTask { customSound.play() }
        },
        "Play",
        null,
        "plays the sound",
        "SoundDebugger Play Sound",
        "Misc",
    ).also {
        Config.registerCategory(it, Categories.DEBUG, "Misc")
    }

    fun initialize() {}
}