package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.ClientThreadServerTickEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature

object SafariUniqueTracker : TextHudFeature(
    "safariUniqueTracker",
    "Tracks the uniques",
    Categories.MISC,
    subcategory = "General",
    area = "safari",
) {
    private val captureRegex = "^CAPTURE! You caught a ([\\w ]+) and gained a? ?(?:\\d+x )?([\\w ]+) Shard!$".toRegex()
    private val teamCaptureRegex = "^LOOT SHARE! You received a? ?(?:\\d+x )?([\\w ]+) Shard from (\\w{1,16}) catching a ([\\w ]+)!$".toRegex()
    private val teamCount = mutableMapOf<String, PlayerData>()
    private var captures = PlayerData(BiomeType.NONE)

    enum class BiomeType(val biomeName: String, val biomeFormat: String, val mobTypes: Set<String>) {
        CAVERN(
            "cavern",
            "&6Cavern",
            setOf(
                "Cavernfish",
                "Flitter",
                "Shyworm",
                "Driftling",
                "Chuckwalla",
                "Rockmite",
                "Scrappy",
                "Snoozle",
                "Gemzie",
            )
        ),
        FOREST(
            "forest",
            "&2Forest",
            setOf(
                "Foxtrot",
                "Bluebird",
                "Honeybug",
                "Treefrog",
                "Woodchucker",
                "Fluffling",
                "Hideonfloor",
                "Parakeet",
                "Macaw",
            )
        ),
        HAUNTED(
            "haunted",
            "&5Haunted",
            setOf(
                "Areita",
                "Bloodbat",
                "Duplico",
                "Gazer",
                "Litterbug",
                "Solsnatcher",
                "Gimmiegold",
                "Hideonwall",
                "Hideyho",
                "Doomspiral",
            )
        ),
        ICY(
            "icy",
            "&9Icy",
            setOf(
                "Strongarm",
                "Tepid",
                "Polaris",
                "Shuddersquid",
                "Billygoat",
                "Mantis Shrimp",
                "Nozzlenose",
                "Troodon",
                "Wumpa",
            )
        ),
        NONE("", "", setOf());

        companion object {
            fun fromMobType(mobType: String): BiomeType? {
                return entries.find { it.mobTypes.contains(mobType) }
            }
        }
    }

    data class PlayerData(
        var biome: BiomeType,
        val captures: MutableSet<String> = mutableSetOf(),
        var isMax: Boolean = false,
    ) {
        fun add(mobType: String) {
            captures.add(mobType)
        }
    }

    override fun initialize() {
        on<ChatEvent> { event ->
            event.matches(teamCaptureRegex)?.let {
                val mobType = it.getOrNull(0) ?: return@on
                val playerName = it.getOrNull(1) ?: return@on
                val biome = BiomeType.fromMobType(mobType) ?: return@on

                teamCount.getOrPut(playerName) { PlayerData(biome) }.add(mobType)
            }
            val ( mobType, shardType ) = event.matches(captureRegex) ?: return@on
            val biome = BiomeType.fromMobType(mobType) ?: return@on
            if (captures.biome == BiomeType.NONE)
                captures.biome = biome

            captures.add(mobType)
        }

        on<ClientThreadServerTickEvent> {
            if (captures.biome == BiomeType.NONE) return@on
            val biome = captures.biome
            val missing = biome.mobTypes - captures.captures

            setLines(buildList {
                add("&e[${biome.biomeFormat}&e] &c${captures.captures.size}&f/&6${biome.mobTypes.size}")
                missing.forEach { add("&7- &c$it") }
                add("")
                teamCount.forEach { (playerName, data) ->
                    val missing = data.biome.mobTypes - data.captures
                    add("&a$playerName &e[${data.biome.biomeFormat}&e]&f: &c${data.captures.size}&f/&6${data.biome.mobTypes.size}")
                    if (missing.size > 3) return@forEach

                    missing.forEach { ms -> add("&7- &c$ms") }
                }
            })
        }

        on<RenderOverlayEvent> {
            draw(it.ctx)
        }
    }

    override fun getEditText(): List<String> = listOf(
        "&e[&6Cavern&e] &c2&f/&69",
        "&7- &cCavernfish",
        "&7- &cFlitter",
        "",
        "&a${minecraft.player?.name?.string ?: ""} &e[&2Forest&e]&f: &c2&f/&69",
    )

    override fun onWorldChange(event: WorldChangeEvent) {
        teamCount.clear()
        captures = PlayerData(BiomeType.NONE)
    }
}