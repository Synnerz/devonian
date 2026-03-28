package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.WebRequests
import com.github.synnerz.devonian.api.dungeon.DungeonClass
import com.github.synnerz.devonian.api.dungeon.PartyFinderListener
import com.github.synnerz.devonian.api.events.ClientThreadServerTickEvent
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.PersistentJson
import com.github.synnerz.devonian.utils.StringUtils
import com.github.synnerz.devonian.utils.StringUtils.colorCodes
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.item.component.ItemLore
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

object PartyFinderOverview : Feature(
    "partyFinderOverview",
    "Customizes the tooltip for party finder parties so they show more information.",
    Categories.PARTY_FINDER,
    searchTags = setOf("pf"),
    subcategory = "Tooltip",
) {
    private const val DUNGEONS_API = "https://dungeons.docilelm.workers.dev/?names="
    private val SETTING_PB_MODE = addSelection(
        "pbMode",
        0,
        listOf("Both", "S", "S+"),
        "The pb mode to use whenever displaying personal best time for the current floor. \"Both\" = if S+ does not exist it'll default to S.",
        "Overview PB",
    )
    private val SETTING_SHOW_MISSING = addSwitch(
        "showMissing",
        true,
        "Shows the missing classes at the bottom of the tooltip",
        "Overview Missing"
    )
    private val SETTING_COMPACT_MODE = addSelection(
        "compactModes",
        0,
        listOf("NONE", "Style1", "Style2", "Custom"),
        "If enabled, it'll compact most of the party finder data from the users",
        "Overview Compact"
    )
    private val SETTING_CUSTOM_STYLE = addTextInput(
        "customStyle",
        "",
        "Only works if \"Custom\" style mode is selected, do /dv pfo help for usage",
        "Overview Custom"
    )
    private val SETTING_COMPACT_NO_NAME = addSwitch(
        "compactNoName",
        false,
        "Whether the Compact Mode can change the color of the igns to their respective class(role) color",
        "Overview Compact Names"
    )
    private val nameRegex = "^§r §r(§\\w)\\w{1,16}§r§f".toRegex()
    private val members = CopyOnWriteArrayList<String>()
    private val cachedMembers = ConcurrentHashMap<String, DungeonsApiResult>()
    private val parties = CopyOnWriteArrayList<PartyFinderListener.PartyFinderData>()

    data class UserDungeonsData(
        val cataXP: Double,
        val level: Double,
        val secrets: Int,
        val averageSecrets: Double,
        val personal_best_normal: Map<String, Map<String, String>>?, // { s: { "floor_1": "1:15" }, s_plus: { "floor_1": "1:15" } }
        val personal_best_master: Map<String, Map<String, String>>?,
    )

    data class DungeonsApiResult(
        var timeTaken: Long,
        val success: Boolean,
        val status: String,
        val data: UserDungeonsData?
    )

    data class MultiDungeonApiResult(val result: Map<String /* player's name */, DungeonsApiResult>?)

    override fun initialize() {
        DevonianCommand.command.subcommand("pfo") { _, args ->
            val type = args.firstOrNull() as? String?
            if (type.isNullOrEmpty()) {
                return@subcommand 0
            }
            ChatUtils.sendMessage(Component.literal("§8[§3§lDevonian§8] §bCustom PartyFinderOverview Style Guide" +
                    "\n§bUsing §6Style1§b as an example§f:" +
                    "\n§f\"&8[§c\$RoleColor§r§e\$RoleSingle&8] §a\$NameColor§r§9\$Name§r &8[&e§d\$RoleLevel§r &7| &6§f\$Cata§r&8] &8[&3§7\$SecretsShort§r &7| &b§2\$SecretShortAvg§r&8] &8[&a§3\$PB§r&8]\"" +
                    "\n§c\"\$RoleColor\" §f-> §ethe role color §8(ex: archer -> &c)" +
                    "\n§e\"\$RoleSingle\" §f-> §eA" +
                    "\n§b\"\$RoleShort\" §f-> §eArch" +
                    "\n§b\"\$Role\" §f-> §eArcher" +
                    "\n§a\"\$NameColor\" §f-> §eeither role color or the username rank color §8(depends on user's settings)" +
                    "\n§9\"\$Name\" §f-> §eplayer's username" +
                    "\n§d\"\$RoleLevel\" §f-> §ethe class level" +
                    "\n§f\"\$Cata\" §f-> §ethe cata level" +
                    "\n§b\"\$Secrets\" §f-> §eunformatted secrets" +
                    "\n§7\"\$SecretsShort\" §f-> §eshortened/formatted secrets" +
                    "\n§b\"\$SecretAvg\" §f-> §eaverage secrets with 2 decimal points" +
                    "\n§2\"\$SecretShortAvg\" §f-> §eaverage secrets with 1 decimal point" +
                    "\n§3\"\$PB\" §f-> §ethe player's personal best §8(depends on user's settings either S/S+ or Both)" +
                    "\n" +
                    "\n§bFor color codes do §6/dv colorcodes"))
            1
        }
            .string("type")
            .suggest("type", *listOf("help").toTypedArray())

        on<PartyFinderListener.PartyFinderEvent> { event ->
            members.clear()
            parties.clear()
            if (event.parties.isEmpty()) return@on

            event.parties.forEach {
                parties.add(it)

                it.members.forEach { m -> members.add(m.name) }
            }
            if (members.isNotEmpty()) {
                WebRequests.withName("PartyFinderOverview") {
                    val filtered = members.filter {
                        val cachedData = cachedMembers[it]
                        cachedData == null ||
                        System.currentTimeMillis() - cachedData.timeTaken >= (1000 * 60 * 60) * 24
                    }
                    if (filtered.isEmpty()) return@withName
                    val result = WebRequests.get("$DUNGEONS_API${filtered.joinToString(",")}")
                    val response: MultiDungeonApiResult = PersistentJson.gson.fromJson(result, MultiDungeonApiResult::class.java) ?: return@withName
                    response.result?.entries?.forEach { (k, v) ->
                        v.timeTaken = System.currentTimeMillis()
                        cachedMembers[k] = v
                    }
                }
            }
        }

        on<ClientThreadServerTickEvent> {
            if (parties.isEmpty()) return@on

            // slightly less efficient workaround to avoid re-set of lore data,
            // although it is still more efficient than doing it inside render tooltip
            parties.forEach { p ->
                val screen = (minecraft.screen as? AbstractContainerScreen<*>) ?: return@on
                val slot = p.idx
                val itemStack = screen.menu.items.getOrNull(slot) ?: return@forEach
                val lore = itemStack.get(DataComponents.LORE) ?: return@forEach
                val newLore = mutableListOf<Component>()

                lore.lines.toList().forEach { l ->
                    val match = PartyFinderListener.USER_ROLE_REGEX.matchEntire(l.string)?.groupValues?.drop(1)
                    if (l.string.contains("Click to join!") || l.string.contains("Requires ")) {
                        if (SETTING_SHOW_MISSING.get()) {
                            val missingComponent = ChatUtils.literal(buildString {
                                append("&eMissing: ")
                                p.missingRoles.forEachIndexed { idx, it ->
                                    if (it == PartyFinderListener.currentRole()) append(if (idx == 0) "&a$it" else "&7, &a$it")
                                    else append(if (idx == 0) "&7$it" else "&7, $it")
                                }
                            })
                            newLore.add(missingComponent)
                        }
                        newLore.add(l.copy())
                        return@forEach
                    }
                    if (l.string.contains("Missing: ")) return@forEach
                    val matchName = nameRegex.find(l.colorCodes())
                    if (match == null) {
                        newLore.add(l.copy())
                        return@forEach
                    }
                    val cache = cachedMembers[match[0]]
                    if (cache == null) {
                        newLore.add(l.copy())
                        return@forEach
                    }
                    val data = cache.data
                    if (data == null) {
                        newLore.add(l.copy())
                        return@forEach
                    }
                    // TODO: this could make the custom break if the user does not use brackets
                    if (l.string.contains("[") && l.string.contains("]")) return@forEach
                    val personalBestMap = if (p.isMasterMode) data.personal_best_master else data.personal_best_normal

                    val ( personalBest, type ) = when (SETTING_PB_MODE.get()) {
                        1 -> personalBestMap?.get("s")?.get("floor_${p.floor}") to "S"
                        2 -> personalBestMap?.get("s_plus")?.get("floor_${p.floor}") to "S+"
                        else -> (personalBestMap?.get("s_plus")?.get("floor_${p.floor}")?.let { it to "S+" })
                            ?: (personalBestMap?.get("s")?.get("floor_${p.floor}") to "S")
                    }

                    val mut = when (SETTING_COMPACT_MODE.get()) {
                        1 -> {
                            ChatUtils.literal(buildString {
                                val role = DungeonClass.from(match[1])
                                val roleCode = role.colorCode
                                val nameColor = if (SETTING_COMPACT_NO_NAME.get() && matchName != null) matchName.groupValues[1] else roleCode
                                append("&8[$roleCode${role.singleLetter.uppercase()}&8] $nameColor${match[0]} &8[&e${match[2]} &7| &6${data.level.toInt()}&8] &8[&3${StringUtils.shortenNumber(data.secrets)} &7| &b${"%.1f".format(data.averageSecrets)}&8]")
                                if (personalBest == null) append(" &8[&cNO PB&8]")
                                else append(" &8[&a$personalBest&8]")
                            })
                        }
                        2 -> {
                            ChatUtils.literal(buildString {
                                val role = DungeonClass.from(match[1])
                                val roleCode = role.colorCode
                                val nameColor = if (SETTING_COMPACT_NO_NAME.get() && matchName != null) matchName.groupValues[1] else roleCode
                                append("&8[$roleCode${role.singleLetter.uppercase()} &e${match[2]}&8] $nameColor${match[0]} &8[&6${data.level.toInt()} &7| &3${StringUtils.shortenNumber(data.secrets)} &7| &b${"%.1f".format(data.averageSecrets)}&8]")
                                if (personalBest == null) append(" &cNO PB")
                                else append(" &a$personalBest")
                            })
                        }
                        3 -> {
                            val role = DungeonClass.from(match[1])
                            val roleCode = role.colorCode
                            val roleSingle = role.singleLetter.uppercase()
                            val roleShort = role.shortName
                            val roleName = role.name
                            val roleLevel = match[2]
                            val cataLevel = data.level
                            val secrets = data.secrets
                            val secretsShort = StringUtils.shortenNumber(data.secrets)
                            val secretsAvg = "%.2f".format(data.averageSecrets)
                            val secretsShortAvg = "%.1f".format(data.averageSecrets)
                            val personalBest = if (personalBest == null) "&cNO PB" else "&a$personalBest"
                            val nameColor = if (SETTING_COMPACT_NO_NAME.get() && matchName != null) matchName.groupValues[1] else roleCode
                            val customKeys = mapOf(
                                "RoleColor" to roleCode,
                                "RoleSingle" to roleSingle,
                                "RoleShort" to roleShort,
                                "Role" to roleName,
                                "RoleName" to roleName,
                                "NameColor" to nameColor,
                                "Name" to match[0],
                                "RoleLevel" to roleLevel,
                                "Cata" to "$cataLevel",
                                "Secrets" to "$secrets",
                                "SecretsShort" to secretsShort,
                                "SecretAvg" to secretsAvg,
                                "SecretShortAvg" to secretsShortAvg,
                                "PB" to personalBest
                            )

                            ChatUtils.literal(Regex("""\$(\w+)""").replace(SETTING_CUSTOM_STYLE.get()) {
                                customKeys[it.groupValues[1]] ?: it.value
                            })
                        }
                        else -> {
                            l.copy()
                                .append(ChatUtils.literal(buildString {
                                    append(" &8(&6${data.level}&8) &8[&3${StringUtils.addCommas(data.secrets)} &7| &b${"%.2f".format(data.averageSecrets)}&8]")
                                    if (personalBest == null) append(" &8[&cNO PB&8]")
                                    else if (SETTING_PB_MODE.get() == 0) append(" &8[&a$type $personalBest&8]")
                                    else append(" &8[&a$personalBest&8]")
                                }))
                        }
                    }

                    newLore.add(mut)
                }

                if (newLore.isEmpty()) return@forEach

                itemStack.set(DataComponents.LORE, ItemLore(newLore.toList()))
            }
        }
    }
}