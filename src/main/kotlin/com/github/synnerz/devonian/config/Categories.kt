package com.github.synnerz.devonian.config

enum class Categories(
    val displayName: String,
    val subcategories: List<String> = listOf("General"),
) {
    GLOBAL("Global", listOf("Mod", "Dungeon Colors", "Chroma")),
    DUNGEONS("Dungeons", listOf("QOL", "Solvers", "HUD", "Alerts", "Highlights", "Hiders")),
    DUNGEON_MAP("Dungeon Map", listOf("Toggle", "Markers", "Colors", "Behavior", "Style")),
    DUNGEON_WAYPOINTS("Dungeon Waypoints", listOf("General", "Custom")),
    PARTY_FINDER("PartyFinder", listOf("General", "Highlight", "Tooltip")),
    F7("F7", listOf("General", "Solvers", "Terminals", "HUD", "Highlight")),
    M7("M7", listOf("General", "Highlight", "HUD")),
    GARDEN("Garden"),
    SLAYERS("Slayers"),
    END("End"),
    DIANA("Diana"),
    MISC("Misc", listOf("General", "Inventory", "Hiders", "Tweaks", "Tooltip", "Actionbar", "Chat")),
    DEBUG("Debug", listOf("Renderers", "Utils", "Packet Logger", "Misc"));

    init {
        if (subcategories.isEmpty()) throw IllegalArgumentException("must provide at least 1 subcategory")
        if (subcategories.size > 8) throw IllegalArgumentException("limit of 8 subcategories")
    }

    companion object {
        fun byName(name: String) = entries.find { it.displayName == name }
    }
}