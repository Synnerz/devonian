package com.github.synnerz.devonian.commands

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.events.ActionbarEvent
import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.api.events.TabUpdateEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.hud.texthud.BImgTextHudRenderer
import com.github.synnerz.devonian.utils.StringUtils.colorCodes
import net.minecraft.network.chat.Component

object DevonianCommand {
    private val commandListeners = mutableListOf<() -> Int>()
    val command = BaseCommand("devonian", listOf("dv")) {
        for (cb in commandListeners) {
            val res = cb()
            if (res != 1) return@BaseCommand res
        }
        1
    }
    private val sendCoordsSub = command.subcommand("sendcoords") { _, args ->
        val pos = Devonian.minecraft.player ?: return@subcommand 0
        val x = pos.x.toInt()
        val y = pos.y.toInt()
        val z = pos.z.toInt()
        ChatUtils.say("x: $x, y: $y, z: $z")
        1
    }
    private val fontChange = command.subcommand("font") { _, args ->
        val fontName = args.getOrNull(0) as? String ?: return@subcommand 0
        if (!BImgTextHudRenderer.Fonts.containsKey(fontName)) {
            ChatUtils.sendMessage("&cInvalid font name", true)
            return@subcommand 0
        }
        BImgTextHudRenderer.setActiveFont(fontName)
        1
    }
        .greedyString("name")
        .suggest("name", *BImgTextHudRenderer.Fonts.keys.toTypedArray())
    private val clearChat = command.subcommand("clearchat") { _, args ->
        Devonian.minecraft.gui.hud.chat.clearMessages(false)
        1
    }
    private val colorCodes = command.subcommand("colorcodes") { _, args ->
        ChatUtils.sendMessage(Component.literal("§8[§3§lDevonian§8] §eMinecraft Color Codes Guide" +
                "\n§4&4 §f- §4Dark Red" +
                "\n§c&c §f- §cRed" +
                "\n§6&6 §f- §6Gold" +
                "\n§e&e §f- §eYellow" +
                "\n§2&2 §f- §2Dark Green" +
                "\n§a&a §f- §aGreen" +
                "\n§3&3 §f- §3Dark Aqua" +
                "\n§b&b §f- §bAqua" +
                "\n§1&1 §f- §1Dark Blue" +
                "\n§9&9 §f- §9Blue" +
                "\n§5&5 §f- §5Dark Purple" +
                "\n§d&d §f- §dLight Purple" +
                "\n§f&f §f- §fWhite" +
                "\n§8&8 §f- §8Dark Gray" +
                "\n§7&7 §f- §7Gray" +
                "\n§0&0 §f- §0Black" +
                "\n§fThe follow are color codes which can be added onto words" +
                "\n§ktest§r &k §f- §fObfuscated" +
                "\n§ltest§r &l §f- §fBold" +
                "\n§mtest§r &m §f- §fStrikethrough" +
                "\n§ntest§r &n §f- §fUnderline" +
                "\n§otest§r &o §f- §fItalic" +
                "\n§r&r §f- §fResets the colors back to default"))
        1
    }
    private val tabListMessages = mutableListOf<Component>()
    private val dumptab = command.subcommand("dumptab") { _, args ->
        tabListMessages.forEach { println("DumpTab(\"${it.string}\", \"${it.colorCodes()}\")") }
        1
    }
    private val actionbarMessages = mutableListOf<Component>()
    private val dumpactionbar = command.subcommand("dumpactionbar") { _, args ->
        actionbarMessages.forEach { println("DumpActionBar(\"${it.string}\", \"${it.colorCodes()}\")") }
        1
    }
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

    fun initialize() {
        EventBus.on<TabUpdateEvent> { event ->
            if (tabListMessages.size >= 100)
                tabListMessages.removeFirst()

            tabListMessages.add(event.comp)
        }

        EventBus.on<ActionbarEvent> { event ->
            if (actionbarMessages.size >= 100)
                actionbarMessages.removeFirst()

            actionbarMessages.add(event.text)
        }

        EventBus.on<WorldChangeEvent> {
            tabListMessages.clear()
            actionbarMessages.clear()
        }

        catacombsFloors.forEachIndexed { idx, cmd ->
            BaseCommand("f${idx + 1}") {
                ChatUtils.command("joindungeon $cmd")
                1
            }.register()
            BaseCommand("m${idx + 1}") {
                ChatUtils.command("joindungeon master_$cmd")
                1
            }.register()
        }
        kuudraTiers.forEachIndexed { idx, cmd ->
            BaseCommand("k${idx + 1}") {
                ChatUtils.command("joininstance kuudra_${cmd}")
                1
            }.register()
            BaseCommand("t${idx + 1}") {
                ChatUtils.command("joininstance kuudra_${cmd}")
                1
            }.register()
        }
        command.register()
    }

    fun onRun(cb: () -> Int) {
        commandListeners.add(cb)
    }
}