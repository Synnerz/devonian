package com.github.synnerz.devonian.features.dungeons.pf

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.dungeon.DungeonClass
import com.github.synnerz.devonian.api.dungeon.DungeonsApi
import com.github.synnerz.devonian.api.dungeon.PartyFinderListener
import com.github.synnerz.devonian.api.events.ClientThreadServerTickEvent
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.FixedIdentityMap
import com.github.synnerz.devonian.utils.LiteralDataComponent
import com.github.synnerz.devonian.utils.StringUtils
import com.github.synnerz.devonian.utils.StringUtils.colorCodes
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.ItemLore
import java.util.IdentityHashMap

object PartyFinderOverview : Feature(
    "partyFinderOverview",
    "Customizes the tooltip for party finder parties so they show more information.",
    Categories.PARTY_FINDER,
    searchTags = setOf("pf"),
    subcategory = "Tooltip",
) {
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
    private val SETTING_FORMAT_NO_DATA = addSwitch(
        "formatNoData",
        true,
        "Whether to still format the party members when their API data has not loaded.",
        "Format Without API Data",
    )

    private val nameRegex = "^§r §r(§\\w)(\\w{1,16})§r§f: (?:§\\w)+(\\w+)(?:§\\w)+ \\((?:§\\w)+(\\d+)(?:§\\w)+\\)$".toRegex()

    private val changedTooltips = FixedIdentityMap<ItemStack, ModifiedState>(21)

    private enum class ModifiedState(val done: Boolean, val addedMissing: Boolean) {
        UNMODIFIED(false, false) {
            override fun addMissing(): ModifiedState = NO_PLAYER_DATA
        },
        NO_PLAYER_DATA(false, true),
        EVERYTHING(true, true);

        open fun addMissing(): ModifiedState = this
    }

    private data class PartyMemberData(val name: String, val nameColor: String, val role: String, val roleLevel: String)

    override fun initialize() {
        DevonianCommand.command.subcommand("pfo") { _, args ->
            val type = args.firstOrNull() as? String?
            if (type.isNullOrEmpty()) {
                return@subcommand 0
            }
            if (type == "test") {
                val message = args.getOrNull(1) as? String? ?: return@subcommand 0
                ChatUtils.sendMessage(
                    customComponent(
                        DungeonsApi.UserDungeonsData(
                            6642506.866023964,
                            32.56,
                            mapOf(
                                "archer" to mapOf(
                                    "xp" to 491314.3586887582,
                                    "level" to 24.01
                                )
                            ),
                            14556,
                            30.35,
                            emptyMap(),
                            emptyList(),
                            emptyList(),
                            1026,
                            emptyMap(),
                            mapOf(
                                "s" to mapOf("floor_3" to "6:03"),
                                "s_plus" to mapOf("floor_3" to "6:03"),
                            )
                        ),
                        PartyFinderListener.PartyFinderMember(
                            minecraft.player!!.name.string,
                            DungeonClass.Archer,
                            24,
                        ),
                        masterMode = true,
                        floor = 3,
                        style = message,
                        nameColor = "§b",
                        memberData = PartyMemberData(
                            minecraft.player!!.name.string,
                            "§b",
                            "Archer",
                            "24",
                        ),
                    )
                )
                return@subcommand 1
            }

            ChatUtils.sendMessage(
                Component.literal(
                    "§8[§3§lDevonian§8] §bCustom PartyFinderOverview Style Guide" +
                    "\n§bUsing §6Style1§b as an example§f:" +
                    $$"\n§f\"&8[§c$RoleColor§r§e$RoleSingle&8] §a$NameColor§r§9$Name§r &8[&e§d$RoleLevel§r &7| &6§f$Cata§r&8] &8[&3§7$SecretsShort§r &7| &b§2$SecretShortAvg§r&8] &8[&a§3$PB§r&8]\"" +
                    $$"\n§c\"$RoleColor\" §f-> §ethe role color §8(ex: archer -> &c)" +
                    $$"\n§e\"$RoleSingle\" §f-> §eA" +
                    $$"\n§b\"$RoleShort\" §f-> §eArch" +
                    $$"\n§b\"$Role\" §f-> §eArcher" +
                    $$"\n§a\"$NameColor\" §f-> §eeither role color or the username rank color §8(depends on user's settings)" +
                    $$"\n§9\"$Name\" §f-> §eplayer's username" +
                    $$"\n§d\"$RoleLevel\" §f-> §ethe class level" +
                    $$"\n§f\"$Cata\" §f-> §ethe cata level" +
                    $$"\n§b\"$Secrets\" §f-> §eunformatted secrets" +
                    $$"\n§7\"$SecretsShort\" §f-> §eshortened/formatted secrets" +
                    $$"\n§b\"$SecretAvg\" §f-> §eaverage secrets with 2 decimal points" +
                    $$"\n§2\"$SecretShortAvg\" §f-> §eaverage secrets with 1 decimal point" +
                    $$"\n§3\"$PB\" §f-> §ethe player's personal best §8(depends on user's settings either S/S+ or Both)" +
                    "\n" +
                    "\n§bFor color codes do §6/dv colorcodes"))
            1
        }
            .string("type")
            .greedyString("other")
            .suggest("type", *listOf("help", "test").toTypedArray())
            .suggest("other", *listOf($$"&8[§c$RoleColor§r§e$RoleSingle&8] §a$NameColor§r§9$Name§r &8[&e§d$RoleLevel§r &7| &6§f$Cata§r&8] &8[&3§7$SecretsShort§r &7| &b§2$SecretShortAvg§r&8] &8[&a§3$PB§r&8]").toTypedArray())

        on<PartyFinderListener.PartyFinderScannedEvent> { event ->
            DungeonsApi.ensureCache(event.parties.flatMap { it.members.map { it.name} })
        }

        on<ClientThreadServerTickEvent> {
            val screen = (minecraft.gui.screen() as? AbstractContainerScreen<*>) ?: return@on
            // slightly less efficient workaround to avoid re-set of lore data,
            // although it is still more efficient than doing it inside render tooltip
            PartyFinderListener.currentParties.forEach { p ->
                val slot = p.idx
                val itemStack = screen.menu.items.getOrNull(slot) ?: return@forEach
                var state = changedTooltips[itemStack] ?: ModifiedState.UNMODIFIED
                if (state.done) return@forEach
                val lore = itemStack.get(DataComponents.LORE) ?: return@forEach
                val newLore = mutableListOf<Component>()

                var missingPlayerData = false
                lore.lines.toList().forEach { l ->
                    val str = l.string
                    if (
                        str.startsWith("Click to join!") ||
                        str.startsWith("Requires ") ||
                        str.startsWith("Complete previous floor first!")
                    ) {
                        if (SETTING_SHOW_MISSING.get() && !state.addedMissing) {
                            val missingComponent = ChatUtils.literal(buildString {
                                append("&eMissing: ")
                                p.missingRoles.forEachIndexed { idx, it ->
                                    val color = if (it == PartyFinderListener.currentRole) "&a" else "&7"
                                    if (idx > 0) append("&7, ")
                                    append(color)
                                    append(it.name)
                                }
                            })
                            newLore.add(missingComponent)
                            state = state.addMissing()
                        }
                        newLore.add(l)
                        return@forEach
                    }

                    var memberData: PartyMemberData? = null
                    var alreadyModified = false
                    if (l is LiteralDataComponent<*>) {
                        val d = l.data
                        if (d is PartyMemberData) {
                            memberData = d
                            alreadyModified = true
                        }
                    } else {
                        val match = nameRegex.matchAt(l.colorCodes(), 0)?.groupValues
                        if (match != null) memberData = PartyMemberData(
                            match[2],
                            match[1],
                            match[3],
                            match[4],
                        )
                    }

                    if (memberData == null) {
                        newLore.add(l)
                        return@forEach
                    }

                    val cache = memberData.name.let { DungeonsApi.getFromCache(it) }
                    var data = cache?.player?.data
                    if (data == null) {
                        missingPlayerData = true
                        if (alreadyModified || !SETTING_FORMAT_NO_DATA.get()) {
                            newLore.add(l)
                            return@forEach
                        }
                        data = DungeonsApi.UserDungeonsData.EMPTY
                    }

                    val personalBestMap = if (p.isMasterMode) data.personal_best_master else data.personal_best_normal

                    val ( personalBest, type ) = when (SETTING_PB_MODE.get()) {
                        1 -> personalBestMap?.get("s")?.get("floor_${p.floor}") to "S"
                        2 -> personalBestMap?.get("s_plus")?.get("floor_${p.floor}") to "S+"
                        else -> (personalBestMap?.get("s_plus")?.get("floor_${p.floor}")?.let { it to "S+" })
                            ?: (personalBestMap?.get("s")?.get("floor_${p.floor}") to "S")
                    }

                    val mut = when (SETTING_COMPACT_MODE.get()) {
                        1 -> {
                            LiteralDataComponent(
                                buildString {
                                    val role = DungeonClass.from(memberData.role)
                                    val roleCode = role.colorCode
                                    val nameColor = if (SETTING_COMPACT_NO_NAME.get()) memberData.nameColor else roleCode

                                    // [A] DocilElm [52 | 60] [120K | 5.4] [NO PB]

                                    append("&8[$roleCode${role.singleLetter.uppercase()}&8] ")
                                    append("$nameColor${memberData.name} ")

                                    append("&8[&e${memberData.roleLevel} &7| ")
                                    append("&6${data.level.toInt()}&8] ")

                                    append("&8[&3${StringUtils.shortenNumber(data.secrets)} &7| ")
                                    append("&b${"%.1f".format(data.averageSecrets)}&8]")

                                    if (personalBest == null) append(" &8[&cNO PB&8]")
                                    else append(" &8[&a$personalBest&8]")
                                },
                                memberData,
                            )
                        }
                        2 -> {
                            LiteralDataComponent(
                                buildString {
                                    val role = DungeonClass.from(memberData.role)
                                    val roleCode = role.colorCode
                                    val nameColor = if (SETTING_COMPACT_NO_NAME.get()) memberData.nameColor else roleCode

                                    // [A 52] DocilElm [60 | 120K | 5.4] NO PB

                                    append("&8[$roleCode${role.singleLetter.uppercase()} ")
                                    append("&e${memberData.roleLevel}&8] ")
                                    append("$nameColor${memberData.name} ")

                                    append("&8[&6${data.level.toInt()} &7| ")
                                    append("&3${StringUtils.shortenNumber(data.secrets)} &7| ")
                                    append("&b${"%.1f".format(data.averageSecrets)}&8]")

                                    if (personalBest == null) append(" &cNO PB")
                                    else append(" &a$personalBest")
                                },
                                memberData,
                            )
                        }
                        3 -> {
                            customComponent(
                                data,
                                p.members.find { it.name == memberData.name }!!,
                                masterMode = p.isMasterMode,
                                floor = p.floor,
                                nameColor = memberData.nameColor,
                                memberData = memberData,
                            )
                        }
                        else -> {
                            l.copy().append(
                                LiteralDataComponent(
                                    buildString {
                                        // DocilElm: Archer (52) (60) [120K | 5.4] [NO PB]

                                        append(" &8(&6${data.level}&8) ")

                                        append("&8[&3${StringUtils.addCommas(data.secrets)} &7| ")
                                        append("&b${"%.2f".format(data.averageSecrets)}&8]")

                                        if (personalBest == null) append(" &8[&cNO PB&8]")
                                        else if (SETTING_PB_MODE.get() == 0) append(" &8[&a$type $personalBest&8]")
                                        else append(" &8[&a$personalBest&8]")
                                    },
                                    memberData,
                                )
                            )
                        }
                    }

                    newLore.add(mut)
                }

                if (newLore.isEmpty()) return@forEach

                if (!missingPlayerData) state = ModifiedState.EVERYTHING
                changedTooltips[itemStack] = state

                itemStack.set(DataComponents.LORE, ItemLore(newLore))
            }
        }
    }

    private fun customComponent(
        data: DungeonsApi.UserDungeonsData,
        playerData: PartyFinderListener.PartyFinderMember,
        style: String = SETTING_CUSTOM_STYLE.get(),
        masterMode: Boolean = false,
        floor: Int = 1,
        nameColor: String?,
        memberData: PartyMemberData,
    ): Component {
        val personalBestMap = if (masterMode) data.personal_best_master else data.personal_best_normal
        val ( personalBest, type ) = when (SETTING_PB_MODE.get()) {
            1 -> personalBestMap?.get("s")?.get("floor_${floor}") to "S"
            2 -> personalBestMap?.get("s_plus")?.get("floor_${floor}") to "S+"
            else -> (personalBestMap?.get("s_plus")?.get("floor_${floor}")?.let { it to "S+" })
                ?: (personalBestMap?.get("s")?.get("floor_${floor}") to "S")
        }

        val role = playerData.role
        val roleCode = role.colorCode
        val roleSingle = role.singleLetter.uppercase()
        val roleShort = role.shortName
        val roleName = role.name
        val roleLevel = playerData.level
        val cataLevel = data.level
        val secrets = data.secrets
        val secretsShort = StringUtils.shortenNumber(data.secrets)
        val secretsAvg = "%.2f".format(data.averageSecrets)
        val secretsShortAvg = "%.1f".format(data.averageSecrets)
        val pb = if (personalBest == null) "&cNO PB" else "&a$personalBest"
        val nameColor1 = if (SETTING_COMPACT_NO_NAME.get() && nameColor != null) nameColor else roleCode
        val customKeys = mapOf(
            "RoleColor" to roleCode,
            "RoleSingle" to roleSingle,
            "RoleShort" to roleShort,
            "Role" to roleName,
            "RoleName" to roleName,
            "NameColor" to nameColor1,
            "Name" to playerData.name,
            "RoleLevel" to "$roleLevel",
            "Cata" to "$cataLevel",
            "Secrets" to "$secrets",
            "SecretsShort" to secretsShort,
            "SecretAvg" to secretsAvg,
            "SecretShortAvg" to secretsShortAvg,
            "PB" to pb
        )

        return LiteralDataComponent(
            Regex("""\$(\w+)""").replace(style) {
                customKeys[it.groupValues[1]] ?: it.value
            },
            memberData,
        )
    }
}