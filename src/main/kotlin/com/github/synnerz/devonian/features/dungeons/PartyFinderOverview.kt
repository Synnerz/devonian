package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.WebRequests
import com.github.synnerz.devonian.api.dungeon.PartyFinderListener
import com.github.synnerz.devonian.api.events.ServerTickEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.PersistentJson
import com.github.synnerz.devonian.utils.StringUtils
import kotlinx.coroutines.launch
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.world.item.component.ItemLore
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

object PartyFinderOverview : Feature(
    "partyFinderOverview",
    "Customizes the tooltip for party finder parties so they show more information (only parties you can join currently)",
    Categories.DUNGEONS,
    subcategory = "QOL"
) {
    private const val DUNGEONS_API = "https://dungeons.docilelm.workers.dev/?name="
    private val SETTING_PB_MODE = addSelection(
        "pbMode",
        0,
        listOf("Both", "S", "S+"),
        "The pb mode to use whenever displaying personal best time for the current floor. \"Both\" = if S+ does not exist it'll default to S",
        "Party Finder Overview PB"
    )
    private val members = CopyOnWriteArrayList<String>()
    private val cachedMembers = ConcurrentHashMap<String, DungeonsApiResult>()
    private val parties = CopyOnWriteArrayList<PartyFinderListener.PartyFinderData>()

    data class UserDungeonsData(
        val cataXP: Double,
        val level: Double,
        val secrets: Int,
        val averageSecrets: Double,
        val personal_best_normal: Map<String, Map<String, String>>, // { s: { "floor_1": "1:15" }, s_plus: { "floor_1": "1:15" } }
        val personal_best_master: Map<String, Map<String, String>>,
    )

    data class DungeonsApiResult(
        var timeTaken: Long,
        val success: Boolean,
        val status: String,
        val data: UserDungeonsData
    )

    override fun initialize() {
        Scheduler.schedulePool.scheduleWithFixedDelay(::onUpdate, 5L, 5L, TimeUnit.SECONDS)

        on<PartyFinderListener.PartyFinderEvent> { event ->
            members.clear()
            parties.clear()
            if (event.parties.isEmpty()) return@on

            event.parties.forEach {
                if (
                    it.canJoin != PartyFinderListener.PartyFinderStatus.CAN_JOIN &&
                    it.canJoin != PartyFinderListener.PartyFinderStatus.DUPE_CLASS
                ) return@forEach

                parties.add(it)

                it.members.forEach { m -> members.add(m.name) }
            }
        }

        on<ServerTickEvent> { event ->
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
                    if (match == null) {
                        newLore.add(l.copy())
                        return@forEach
                    }
                    val cache = cachedMembers[match[0]]
                    if (cache == null) {
                        newLore.add(l.copy())
                        return@forEach
                    }
                    if (l.string.contains("[") && l.string.contains("]")) return@forEach
                    val personalBestMap = if (p.isMasterMode) cache.data.personal_best_master else cache.data.personal_best_normal
                    val ( personalBest, type ) = when (SETTING_PB_MODE.get()) {
                        1 -> personalBestMap["s"]?.get("floor_${p.floor}") to "S"
                        2 -> personalBestMap["s_plus"]?.get("floor_${p.floor}") to "S+"
                        else -> (personalBestMap["s_plus"]?.get("floor_${p.floor}")?.let { it to "S+" })
                            ?: (personalBestMap["s"]?.get("floor_${p.floor}") to "S")
                    }

                    val mut = l.copy()
                        .append(Component.literal(" (").withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY)))
                        .append(Component.literal("${cache.data.level}").withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)))
                        .append(Component.literal(") ").withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY)))
                        .append(Component.literal("[").withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY)))
                        .append(Component.literal("${StringUtils.addCommas(cache.data.secrets)}/${"%.2f".format(cache.data.averageSecrets)}").withStyle(ChatFormatting.AQUA))
                        .append(Component.literal("] ").withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY)))
                    if (personalBest != null) {
                        mut
                            .append(Component.literal("[").withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY)))
                            .append(Component.literal("$type $personalBest").withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN)))
                            .append(Component.literal("]").withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY)))
                    }

                    newLore.add(mut)
                }

                if (newLore.isEmpty()) return@forEach

                itemStack.set(DataComponents.LORE, ItemLore(newLore.toList()))
            }
        }
    }

    private fun onUpdate() {
        if (members.isEmpty()) return

        WebRequests.ioScope.launch {
            val username = members.removeFirstOrNull() ?: return@launch
            val cachedData = cachedMembers[username]
            if (cachedData != null && System.currentTimeMillis() - cachedData.timeTaken <= (1000 * 60) * 20) return@launch

            val response = WebRequests.get("${DUNGEONS_API}$username")
            val data = PersistentJson.gson.fromJson(response, DungeonsApiResult::class.java)
            data.timeTaken = System.currentTimeMillis()

            cachedMembers[username] = data
        }
    }
}