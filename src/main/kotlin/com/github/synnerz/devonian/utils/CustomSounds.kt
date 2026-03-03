package com.github.synnerz.devonian.utils

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.api.events.SoundPlayEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Config
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvent

object CustomSounds {
    val sounds = mutableListOf<CustomSound>()
    var queued: CustomSound? = null

    init {
        EventBus.on<WorldChangeEvent> {
            queued = null
        }
    }

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
            soundEvent = BuiltInRegistries.SOUND_EVENT.getValue(Identifier.parse(value))

            Config.onAfterLoad {
                Config.get<Float>("$key\$Volume")?.let { setVolume(it) }
                Config.get<Float>("$key\$Pitch")?.let { setPitch(it) }
                Config.get<String>(key)?.let { setValue(it) }
            }

            sounds.add(this)
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
            soundEvent = BuiltInRegistries.SOUND_EVENT.getValue(Identifier.parse(value))
        }

        fun setValues(value: String, volume: Float, pitch: Float) {
            setValue(value)
            setVolume(volume)
            setPitch(pitch)
        }

        fun isLoud() = volume > 1f

        fun que() {
            queued = this
        }

        fun unque() {
            queued = null
        }

        inline fun quePop(block: () -> Unit) {
            que()
            block()
            unque()
        }

        /**
         * * NOTE: call on main thread
         */
        fun play() {
            quePop {
                soundEvent?.let { Devonian.minecraft.player?.playSound(it, volume, pitch) }
            }
        }

        /**
         * * NOTE: call on main thread
         */
        fun playWithEvent(event: SoundPlayEvent) {
            soundEvent?.let {
                quePop {
                    Devonian.minecraft.level?.playLocalSound(
                        event.x, event.y, event.z,
                        it, event.category,
                        volume, pitch,
                        false
                    )
                }
            }
        }
    }
}