package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.dungeon.DungeonsApi
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.StringUtils
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style

object PartyFinderStats : Feature(
    "partyFinderStats",
    "Displays the player's stats whenever they JOIN your party (not to be confused with §bPartyFinderOverview§r)",
    Categories.PARTY_FINDER,
    subcategory = "General",
) {
    private val partyFinderJoinRegex = "^Party Finder > (\\w{1,16}) joined the dungeon group! \\((?:Healer|Tank|Mage|Berserk|Archer) Level \\d+\\)$".toRegex()
    private val roleFormat = mapOf(
        "archer" to "&6☣ Archer",
        "mage" to "&b✎ Mage",
        "berserk" to "&c⚔ Berserker",
        "healer" to "&a❤ Healer",
        "tank" to "&7❈ Tank",
    )
    private var lastUsername = ""

    override fun initialize() {
        DungeonsApi.on { name, data ->
            if (lastUsername.isEmpty() || name != lastUsername) return@on

            onData(data, name)
            lastUsername = ""
        }

        on<ChatEvent> { event ->
            val ( username ) = event.matches(partyFinderJoinRegex) ?: return@on
            if (username == Dungeons.selfPlayer.name) return@on
            val cache = DungeonsApi.player(username)
            if (cache == null) {
                lastUsername = username
                return@on
            }

            onData(cache, username)
        }
    }

    private fun onData(data: DungeonsApi.DungeonsApiResult, username: String) {
        ChatUtils.sendMessage("&b${username}'s &eStats", true)
        ChatUtils.sendMessage("&7-  &cCata&f: &6${data.level()}")
        data.roles().entries.forEach { (roleName, values) ->
            val level = values["level"] ?: 0.0
            ChatUtils.sendMessage("  ${roleFormat[roleName]}&f: &6${level}")
        }
        ChatUtils.sendMessage(ChatUtils.literal("&7-  &aNormal &dPersonal Best &7(hover)").setStyle(
            Style.EMPTY.withHoverEvent(HoverEvent.ShowText(ChatUtils.literal(buildString {
                data.normalPBs().forEach { (pbMode, values) ->
                    append(if (pbMode == "s_plus") "\n&6S+\n" else "&eS\n")
                    values.forEach { (floor, time) ->
                        if (floor.endsWith("_best") || floor.endsWith("_ms")) return@forEach
                        append("&a${floor.replace("_", " ")}&f: &6$time\n")
                    }
                }
            }.trim())))
        ))
        ChatUtils.sendMessage(ChatUtils.literal("&7-  &cMaster &dPersonal Best &7(hover)").setStyle(
            Style.EMPTY.withHoverEvent(HoverEvent.ShowText(ChatUtils.literal(buildString {
                data.masterPBs().forEach { (pbMode, values) ->
                    append(if (pbMode == "s_plus") "\n&6S+\n" else "&eS\n")
                    values.forEach { (floor, time) ->
                        if (floor.endsWith("_best") || floor.endsWith("_ms")) return@forEach
                        append("&c${floor.replace("_", " ")}&f: &6$time\n")
                    }
                }
            }.trim())))
        ))
        ChatUtils.sendMessage("&7-  &bSecrets&f: &6${StringUtils.addCommas(data.secrets())} &7(${"%.2f".format(data.averageSecrets())})")
        ChatUtils.sendMessage("&7-  &bMagical Power&f: &6${StringUtils.addCommas(data.magicalPower())}")
        val spiritPetFormat = when {
            data.spirit().isEmpty() || data.spirit()["tier"] == null -> "&7&m"
            data.spirit()["tier"] == "EPIC" -> "&5"
            data.spirit()["tier"] == "LEGENDARY" -> "&6"
            else -> ""
        }
        val gdrags = buildList {
            data.goldenDragon().forEach {
                val heldItem = it["heldItem"] ?: return@forEach
                add("&6Golden Dragon&7(${heldItem})")
            }
        }
        val edrags = buildList {
            data.enderDragon().forEach {
                val tier = it["tier"]
                val heldItem = it["heldItem"]
                val nameFormat = if (tier == "LEGENDARY") "&6" else if (tier == "EPIC") "&5" else return@forEach
                add("${nameFormat}Ender Dragon&7($heldItem)")
            }
        }
        ChatUtils.sendMessage(buildString {
            append("&7-  &dPets&f: ${spiritPetFormat}Spirit&r")
            if (gdrags.isNotEmpty())
                append("&f, ${gdrags.joinToString("&f, ")}")
            if (edrags.isNotEmpty())
                append("&f, ${edrags.joinToString("&f, ")}")
        })
        ChatUtils.sendMessage(ChatUtils.literal("                 &cClick To Kick &e${username}").withStyle(
            Style.EMPTY.withClickEvent(ClickEvent.RunCommand("p kick $username"))
        ))
    }
}