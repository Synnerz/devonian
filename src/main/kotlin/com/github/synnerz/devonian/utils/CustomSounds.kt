package com.github.synnerz.devonian.utils

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.config.Config
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent

object CustomSounds {
    fun create(key: String, default: String): CustomSound = CustomSound(key, default, 1f, 1f)

    data class CustomSound(
        val key: String,
        @JvmField
        var value: String,
        @JvmField
        var volume: Float,
        @JvmField
        var pitch: Float,
        var soundEvent: SoundEvent? = null,
    ) {
        init {
            Config.set(key, value)
            Config.set("$key\$Volume", 1f)
            Config.set("$key\$Pitch", 1f)
            soundEvent = BuiltInRegistries.SOUND_EVENT.getValue(ResourceLocation.parse(value))

            Config.onAfterLoad {
                Config.get<Float>("$key\$Volume")?.let { setVolume(it) }
                Config.get<Float>("$key\$Pitch")?.let { setPitch(it) }
                Config.get<String>(key)?.let { setValue(it) }
            }
        }

        fun setVolume(volume: Float) {
            this.volume = volume
            Config.set("$key\$Volume", volume)
        }

        fun setPitch(pitch: Float) {
            this.pitch = pitch
            Config.set("$key\$Pitch", pitch)
        }

        fun setValue(value: String) {
            this.value = value
            Config.set(key, value)
            soundEvent = BuiltInRegistries.SOUND_EVENT.getValue(ResourceLocation.parse(value))
        }

        fun setValues(value: String, volume: Float, pitch: Float) {
            setValue(value)
            setVolume(volume)
            setPitch(pitch)
        }

        /**
         * * NOTE: always call me on main thread
         */
        fun play() {
            soundEvent?.let { Devonian.minecraft.player?.playSound(it, volume, pitch) }
        }
    }
}