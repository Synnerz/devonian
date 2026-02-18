package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.WebRequests
import com.github.synnerz.devonian.api.dungeon.DungeonClass
import com.github.synnerz.devonian.api.dungeon.PartyFinderListener
import com.github.synnerz.devonian.api.events.ClientThreadServerTickEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.PersistentJson
import com.github.synnerz.devonian.utils.StringUtils
import com.github.synnerz.devonian.utils.StringUtils.colorCodes
import kotlinx.coroutines.launch
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.item.component.ItemLore
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

object PartyFinderOverview : Feature(
    "partyFinderOverview",
    "Customizes the tooltip for party finder parties so they show more information.",
    Categories.DUNGEONS,
    subcategory = "QOL",
    searchTags = setOf("pf"),
) {
    private const val DUNGEONS_API = "https://dungeons.docilelm.workers.dev/?names="
    private val SETTING_PB_MODE = addSelection(
        "pbMode",
        0,
        listOf("Both", "S", "S+"),
        "The pb mode to use whenever displaying personal best time for the current floor. \"Both\" = if S+ does not exist it'll default to S.",
        "Party Finder Overview PB",
    )
    private val SETTING_SHOW_MISSING = addSwitch(
        "showMissing",
        true,
        "Shows the missing classes at the bottom of the tooltip",
        "Party Finder Overview Missing"
    )
    private val SETTING_COMPACT_MODE = addSelection(
        "compactModes",
        0,
        listOf("NONE", "Style1", "Style2"),
        "If enabled, it'll compact most of the party finder data from the users",
        "Party Finder Overview Compact"
    )
    private val SETTING_COMPACT_NO_NAME = addSwitch(
        "compactNoName",
        false,
        "Whether the Compact Mode can change the color of the igns to their respective class(role) color",
        "Party Finder Overview Compact Names"
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
        on<PartyFinderListener.PartyFinderEvent> { event ->
            members.clear()
            parties.clear()
            if (event.parties.isEmpty()) return@on

            event.parties.forEach {
                parties.add(it)

                it.members.forEach { m -> members.add(m.name) }
            }
            if (members.isNotEmpty()) {
                WebRequests.ioScope.launch {
                    val filtered = members.filter {
                        val cachedData = cachedMembers[it]
                        cachedData == null ||
                        System.currentTimeMillis() - cachedData.timeTaken >= (1000 * 60 * 60) * 24
                    }
                    if (filtered.isEmpty()) return@launch
                    val result = WebRequests.get("$DUNGEONS_API${filtered.joinToString(",")}")
                    val response: MultiDungeonApiResult = PersistentJson.gson.fromJson(result, MultiDungeonApiResult::class.java) ?: return@launch
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