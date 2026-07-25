package com.github.synnerz.devonian.utils

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.api.events.SoundPlayEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.config.Config
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource

object CustomSounds {
    val sounds = mutableListOf<CustomSound>()
    var queued: CustomSound? = null

    init {
        EventBus.on<WorldChangeEvent> {
            queued = null
        }
    }

    fun create(key: String, default: String): CustomSound
        = CustomSound(key, default, 1f, 1f)

    fun createFake(key: String, default: String): CustomSound
        = CustomSound(key, default, 1f, 1f, isFake = true)

    data class CustomSound(
        val key: String,
        @JvmField
        var value: String,
        @JvmField
        var volume: Float,
        @JvmField
        var pitch: Float,
        var soundEvent: SoundEvent? = null,
        val isFake: Boolean = false,
    ) {
        init {
            if (!isFake) {
                Config.set(key, value)
                Config.set("$key\$Volume", 1f)
                Config.set("$key\$Pitch", 1f)

                Config.onAfterLoad {
                    Config.get<Float>("$key\$Volume")?.let { setVolume(it) }
                    Config.get<Float>("$key\$Pitch")?.let { setPitch(it) }
                    Config.get<String>(key)?.let { setValue(it) }
                }
            }

            soundEvent = soundEvent ?: BuiltInRegistries.SOUND_EVENT.getValue(Identifier.parse(value))
            sounds.add(this)
        }

        @JvmOverloads
        fun registerCommand(
            name: String,
            suggestions: List<String> = listOf(
                "minecraft:entity.blaze.hurt",
                "minecraft:entity.experience_orb.pickup",
                "minecraft:block.vault.break",
                "minecraft:entity.elder_guardian.hurt_land",
                "minecraft:item.totem.use",
                "minecraft:block.sculk_catalyst.hit",
                "minecraft:block.ender_chest.close",
                "minecraft:block.note_block.iron_xylophone",
            )
        ) {
            DevonianCommand.command.subcommand(name) { _, args ->
                val volume = args.firstOrNull() as? Float?
                val pitch = args.getOrNull(1) as? Float?
                var soundName = args.getOrNull(2) as? String
                if (soundName.isNullOrEmpty()) soundName = value

                setValues(soundName, volume ?: 1f, pitch ?: 1f)

                if (soundEvent == null) {
                    ChatUtils.sendMessage("&4Cannot find sound: &a$soundName", true)
                    return@subcommand 0
                }

                ChatUtils.sendMessage("&aSuccessfully set sound to &a$soundName ${volume}v ${pitch}p", true)
                1
            }
                .float("volume", 0f, 10f)
                .float("pitch", 0f, 10f)
                .greedyString("sound")
                .suggest(
                    "sound",
                    *suggestions.toTypedArray()
                )
        }

        fun setVolume(volume: Float) {
            this.volume = volume
            if (!isFake) Config.set("$key\$Volume", volume)
        }

        fun setPitch(pitch: Float) {
            this.pitch = pitch
            if (!isFake) Config.set("$key\$Pitch", pitch)
        }

        fun setValue(value: String) {
            this.value = value
            if (!isFake) Config.set(key, value)
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
         * * If master is set to false it will default to PLAYER as source
         */
        @JvmOverloads
        fun play(masterSource: Boolean = true) {
            quePop {
                soundEvent?.let {
                    Devonian.minecraft.player?.let { pl ->
                        Devonian.minecraft.level?.playLocalSound(
                            pl.x, pl.y, pl.z,
                            it,
                            if (masterSource) SoundSource.MASTER
                            else SoundSource.PLAYERS,
                            volume, pitch,
                            false,
                        )
                    }
                }
            }
        }

        /**
         * * NOTE: call on main thread
         */
        @JvmOverloads
        fun playWithEvent(event: SoundPlayEvent, eventCategory: Boolean = true) {
            soundEvent?.let {
                quePop {
                    Devonian.minecraft.level?.playLocalSound(
                        event.x, event.y, event.z,
                        it,
                        if (eventCategory) event.category
                        else SoundSource.MASTER,
                        volume, pitch,
                        false
                    )
                }
            }
        }
    }
}