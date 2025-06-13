package com.github.synnerz.devonian.features

import com.github.synnerz.devonian.Devonian
import net.minecraft.text.Text

// TODO: make this more useful
abstract class Feature {
    val minecraft = Devonian.minecraft

    abstract fun initialize()

    fun sendMessage(msg: String) {
        minecraft.player?.sendMessage(Text.literal(msg), false)
    }
}