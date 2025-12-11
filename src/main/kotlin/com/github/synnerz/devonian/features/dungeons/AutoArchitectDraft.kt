package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.features.Feature

object AutoArchitectDraft : Feature(
    "AutoArchitectDraft",
    "Automatically sends the gfs command whenever YOU fail a puzzle",
    "Dungeons",
    "catacombs"
) {
    // PUZZLE FAIL! DocilElm lost Tic Tac Toe! Yikes!
    // PUZZLE FAIL! DocilElm killed a Blaze in the wrong order! Yikes!
    private val puzzleFailedRegex = "^PUZZLE FAIL! (\\w{1,16}) .*$".toRegex()
    // [STATUE] Oruo the Omniscient: DocilElm chose the wrong answer! I shall never forget this moment of misrememberance.
    private val quizFailedRegex = "^\\[STATUE] Oruo the Omniscient: (\\w{1,16}) chose the wrong answer! I shall never forget this moment of misrememberance\\.$".toRegex()

    override fun initialize() {
        DevonianCommand.command.subcommand("draft") { _, args ->
            pickupDraft()
            1
        }

        on<ChatEvent> { event ->
            event.matches(quizFailedRegex)?.let {
                val name = it[0]
                if (name != minecraft.player!!.name.string) return@on

                pickupDraft()
                return@on
            }

            val match = event.matches(puzzleFailedRegex) ?: return@on
            val name = match[0]
            if (name != minecraft.player!!.name.string) return@on

            pickupDraft()
        }
    }

    private fun pickupDraft() {
        ChatUtils.command("gfs architect's first draft 1")
    }
}