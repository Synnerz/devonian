package com.github.synnerz.devonian.config

import com.github.synnerz.devonian.Devonian.features
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.commands.BaseCommand
import com.github.synnerz.devonian.utils.ChatUtils
import net.minecraft.text.ClickEvent
import net.minecraft.text.MutableText
import net.minecraft.text.Style

object Config {
    private val featuresSize get() = features.size
    private val minPage = 0
    private val maxPage get() = featuresSize / 10
    private var currentPage = 0

    fun initialize() {
        val baseCommand = BaseCommand("devonian") {
            ChatUtils.sendMessage("&7Settings", true)
            for (feature in currentFeatures()) {
                feature.displayChat()
            }
            ChatUtils.sendMessageWithId(pageLiteral(), 100)
            1
        }

        baseCommand.subcommand("config", true) { _, args ->
            if (args.isEmpty()) {
                ChatUtils.sendMessage("&7Settings", true)
                for (feature in currentFeatures()) {
                    feature.displayChat()
                }
                return@subcommand 1
            }

            val id = args.first() as Int

            if (id < 256652) {
                if (id > maxPage || id < minPage) return@subcommand 1
                for (feature in currentFeatures())
                    feature.deleteChat()

                if (id < currentPage) currentPage--.coerceAtLeast(minPage)
                else if (id != currentPage) currentPage++.coerceAtMost(maxPage)

                for (feature in currentFeatures())
                    feature.displayChat()

                ChatUtils.removeLines { ChatUtils.chatLineIds[it] == 100 }
                ChatUtils.sendMessageWithId(pageLiteral(), 100)
                return@subcommand 1
            }

            for (feature in currentFeatures()) {
                feature.onCommand(id)
            }
            1
        }.integer("\$configid")

        baseCommand.register()
    }

    private fun currentFeatures(): MutableList<Feature> {
        val list = mutableListOf<Feature>()

        for (n in 0..10) {
            val idx = n + (currentPage * 10)
            if (featuresSize < idx + 1) break
            list.add(features[idx])
        }

        return list
    }

    private fun pageLiteral(): MutableText {
        val previousPage = ChatUtils.literal("&7<<  &f${currentPage + 1}&f/")
            .setStyle(Style.EMPTY.withClickEvent(
                ClickEvent.RunCommand("devonian config ${(currentPage - 1).coerceAtLeast(minPage)}"))
            )
        val nextPage = ChatUtils.literal("&b${maxPage + 1}  &f>>")
            .setStyle(
                Style.EMPTY.withClickEvent(
                    ClickEvent.RunCommand("devonian config ${(currentPage + 1).coerceAtMost(maxPage)}"))
            )

        return ChatUtils.literal("   ").append(previousPage).append(nextPage)
    }
}