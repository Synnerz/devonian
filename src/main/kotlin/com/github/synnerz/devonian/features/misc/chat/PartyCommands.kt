package com.github.synnerz.devonian.features.misc.chat

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.*
import com.github.synnerz.devonian.api.events.ChatChannelEvent
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.features.misc.TpsDisplay
import com.github.synnerz.devonian.utils.PersistentJsonClass
import com.google.gson.reflect.TypeToken

object PartyCommands : Feature(
    "partyCommands",
    "party commands !e, !f<1-7>, !m<1-7>, !t<1-5>, !w, !warp, !fps, !tps, !ping, !location, !allinv, !pt, !inv, !kick",
    subcategory = "Chat"
) {
    private val SETTING_USE_WHITELIST = addSwitch(
        "useWhitelist",
        true,
        "Whether the feature should use a whitelist for !allinv, !pt, !inv, !kick commands (/dv pcmd <username>)",
        "Party Commands Whitelist"
    )
    private val catacombsFloors = listOf(
        "catacombs_floor_one",
        "catacombs_floor_two",
        "catacombs_floor_three",
        "catacombs_floor_four",
        "catacombs_floor_five",
        "catacombs_floor_six",
        "catacombs_floor_seven",
    )
    private val kuudraTiers = listOf(
        "normal",
        "hot",
        "burning",
        "fiery",
        "infernal",
    )
    private val loader = object : PersistentJsonClass<MutableList<String>>(
        "devonian/partycommandswhitelist.json",
        object : TypeToken<MutableList<String>>() {}
    ) {
        override fun onLoadDefault() {
            data = mutableListOf()
        }
    }

    override fun initialize() {
        loader.load()

        DevonianCommand.command.subcommand("pcmd") { _ , args ->
            val first = (args.firstOrNull() as? String?)?.lowercase() ?: return@subcommand 0
            if (first.length !in 1..16) {
                ChatUtils.sendMessage("&cPCMDWhitelist invalid username input", true)
                return@subcommand 0
            }
            val data = loader.data ?: return@subcommand 0
            if (data.contains(first)) {
                data.remove(first)
                ChatUtils.sendMessage("&cPCMDWhitelist removed player of the list with name &b$first", true)
                return@subcommand 1
            }
            data.add(first)
            ChatUtils.sendMessage("&aPCMDWhitelist added player to list with name &b$first", true)
            1
        }.greedyString("username")

        on<ChatChannelEvent.PartyChatEvent> { event ->
            val name = event.name.lowercase()
            val msg = event.userMessage.lowercase()
            if (!msg.startsWith("!")) return@on
            val messages = msg.split(" ")
            val second = messages.getOrNull(1)

            Scheduler.scheduleTask {
                when (val first = messages.first()) {
                    "!w", "!warp" -> if (Party.isLeader) ChatUtils.command("p warp")
                    "!allinv" -> if (canTrigger(name)) ChatUtils.command("p settings allinvite")
                    "!pt" -> if (canTrigger(name)) ChatUtils.command("p transfer $name")
                    "!inv" -> if (canTrigger(name) && second != null && second.length in 1..16) ChatUtils.command("p $second")
                    "!kick" -> if (canTrigger(name) && second != null && second.length in 1..16) ChatUtils.command("p kick $second")
                    "!coords" -> {
                        val pos = Devonian.minecraft.player ?: return@scheduleTask
                        val x = pos.x.toInt()
                        val y = pos.y.toInt()
                        val z = pos.z.toInt()
                        ChatUtils.command("pc x: $x, y: $y, z: $z")
                    }
                    "!fps" -> ChatUtils.command("pc FPS ${minecraft.fps}")
                    "!ping" -> ChatUtils.command("pc Ping: ${"%.2f".format(Ping.getLastPing())}, Avg: ${"%.2f".format(Ping.getMedianPing())}")
                    "!tps" -> ChatUtils.command("pc Tps: ${TpsDisplay.lastCur}, Avg: ${"%.2f".format(TpsDisplay.lastAvg)}")
                    "!location" -> ChatUtils.command("pc Location: ${Location.area} - ${Location.subarea}")
                    "!e" -> if (Party.isLeader) ChatUtils.command("joindungeon catacombs_entrance")
                    else -> {
                        val str = first.getOrNull(1) ?: return@scheduleTask
                        val num = first.getOrNull(2)?.digitToIntOrNull() ?: return@scheduleTask
                        when (str) {
                            'f' -> if (num in 1..7 && Party.isLeader) ChatUtils.command("joindungeon ${catacombsFloors[num - 1]}")
                            'm' -> if (num in 1..7 && Party.isLeader) ChatUtils.command("joindungeon master_${catacombsFloors[num - 1]}")
                            't' -> if (num in 1..5 && Party.isLeader) ChatUtils.command("joininstance kuudra_${kuudraTiers[num - 1]}")
                        }
                    }
                }
            }
        }
    }

    private fun canTrigger(name: String): Boolean {
        if (!SETTING_USE_WHITELIST.get()) return Party.isLeader
        return (name == minecraft.player!!.name.string || loader.data!!.contains(name)) && Party.isLeader
    }
}