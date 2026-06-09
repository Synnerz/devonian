package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.ClientContainerCloseEvent
import com.github.synnerz.devonian.api.events.ClientThreadServerTickEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.ServerContainerCloseEvent
import com.github.synnerz.devonian.api.events.ServerContainerOpenEvent
import com.github.synnerz.devonian.api.events.ServerContainerSetSlotEvent
import com.github.synnerz.devonian.api.events.TabUpdateEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.StringUtils.colorCodes
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.world.item.Items

// TODO: add it to fishing category whenever unlazy
object LotusPityDisplay : TextHudFeature(
    "lotusPityDisplay",
    "Displays your pity trophy frog for lotus atoll (requires you to open /pity -> atoll section at least once, as well as having the tab widget fully visible)",
    Categories.MISC,
    "lotus atoll",
    subcategory = "General",
) {
    private const val KEY_NAME = "FrogPity"
    private val trophyFrogsRegex = "^ ([●○]+) ([\\w ]+) $".toRegex()
    private val pityProgressRegex = "^Progress to (GOLD|DIAMOND): (\\d+)/(\\d+)$".toRegex()
    private val trophyCaughtRegex = "^♔ TROPHY FROG! You caught an? ([\\w ]+) (BRONZE|SILVER|GOLD|DIAMOND)!$".toRegex()
    private val trophyCaughtMultiRegex = "^♔ TROPHY FROG! You caught ([\\w ]+) (BRONZE|SILVER|GOLD|DIAMOND) x(\\d+)!$".toRegex()
    private val pityCount = mutableListOf<PityFrog>()
    private val trophyFrogs = mutableMapOf<String, Pair<Int, String>>()
    private var inPityGui = false
    private var lastCatch: String? = null

    data class PityValue(var amount: Int, val total: Int)
    data class PityFrog(
        val name: String,
        val gold: PityValue,
        val diamond: PityValue,
    )

    override fun initialize() {
        Config.set(KEY_NAME, JsonArray())

        Config.onAfterLoad {
            Config.get<List<JsonObject>>(KEY_NAME)?.forEach {
                val name = it.get("name").asString
                val gold = it.getAsJsonObject("gold")
                val diamond = it.getAsJsonObject("diamond")

                pityCount.add(PityFrog(
                    name,
                    PityValue(gold.get("amount").asInt, gold.get("total").asInt),
                    PityValue(diamond.get("amount").asInt, diamond.get("total").asInt),
                ))
            }
        }

        Config.onPreSave {
            val arr = JsonArray()

            pityCount.forEach {
                val obj = JsonObject()

                obj.addProperty("name", it.name)
                obj.add("gold", JsonObject().apply {
                    addProperty("amount", it.gold.amount)
                    addProperty("total", it.gold.total)
                })
                obj.add("diamond", JsonObject().apply {
                    addProperty("amount", it.diamond.amount)
                    addProperty("total", it.diamond.total)
                })

                arr.add(obj)
            }

            Config.set(KEY_NAME, arr)
        }

        on<TabUpdateEvent> { event ->
            val ( emblems, name ) = event.matches(trophyFrogsRegex) ?: return@on
            trophyFrogs[name] = emblems.count { it == '●' } to event.comp.colorCodes()
        }

        on<ChatEvent> { event ->
            event.matches(trophyCaughtMultiRegex)?.let { (name, type, num) ->
                val amount = num.toIntOrNull() ?: 1

                lastCatch = name

                val pity = pityCount.find { it.name == name } ?: return@on
                when (type) {
                    "GOLD" -> pity.gold.amount = 0
                    "DIAMOND" -> pity.diamond.amount = 0
                    else -> {
                        pity.gold.amount += amount
                        pity.diamond.amount += amount
                    }
                }
                return@on
            }

            val ( name, type ) = event.matches(trophyCaughtRegex) ?: return@on

            lastCatch = name

            val pity = pityCount.find { it.name == name } ?: return@on
            when (type) {
                "GOLD" -> pity.gold.amount = 0
                "DIAMOND" -> pity.diamond.amount = 0
                else -> {
                    pity.gold.amount += 1
                    pity.diamond.amount += 1
                }
            }
        }

        on<ServerContainerOpenEvent> { event ->
            inPityGui = event.titleStr == "Lotus Atoll Pity"
        }

        on<ServerContainerCloseEvent> { inPityGui = false }
        on<ClientContainerCloseEvent> { inPityGui = false }

        on<ServerContainerSetSlotEvent> { event ->
            if (!inPityGui) return@on
            val slot = event.slot
            val itemStack = event.itemStack
            if (slot !in 9..26 || itemStack.item != Items.PLAYER_HEAD) return@on
            val name = itemStack.customName?.string ?: return@on
            val lore = ItemUtils.lore(itemStack) ?: return@on
            var gold: Pair<Int, Int> = 0 to 0
            var diamond: Pair<Int, Int> = 0 to 0

            for (line in lore) {
                val match = pityProgressRegex.matchEntire(line)?.groupValues ?: continue
                if (match[1] == "GOLD") {
                    gold = (match[2].toIntOrNull() ?: 0) to (match[3].toIntOrNull() ?: 0)
                    continue
                }

                diamond = (match[2].toIntOrNull() ?: 0) to (match[3].toIntOrNull() ?: 0)
            }

            pityCount.removeIf { it.name == name }
            pityCount.add(PityFrog(
                name,
                PityValue(gold.first, gold.second),
                PityValue(diamond.first, diamond.second)
            ))
        }

        on<ClientThreadServerTickEvent> {
            if (lastCatch == null) return@on
            val pity = pityCount.find { it.name == lastCatch } ?: return@on
            val data = trophyFrogs[lastCatch] ?: return@on
            val isGold = data.first < 3
            val counter =
                if (isGold) pity.gold
                else pity.diamond
            val typeFormat =
                if (isGold) "&6GOLD"
                else "&bDIAMOND"

            setLine("${data.second} &7($typeFormat&7)&f: &c${counter.amount}&f/&a${counter.total}")
        }

        on<RenderOverlayEvent> {
            draw(it.ctx)
        }
    }

    override fun getEditText(): List<String> = listOf("&8●&7●&6●&b● &fCommon Frog &7(&bDIAMOND&7)&f: &c100&f/&a600")
}