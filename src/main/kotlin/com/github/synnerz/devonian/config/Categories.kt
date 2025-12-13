package com.github.synnerz.devonian.config

enum class Categories(
    val displayName: String,
    val subcategories: List<String> = listOf("General"),
) {
    DUNGEONS("Dungeons", listOf("QOL", "Solvers", "HUD", "World")),
    DUNGEON_MAP("Dungeon Map", listOf("Toggle", "Markers", "Colors", "Behavior", "Style")),
    GARDEN("Garden"),
    SLAYERS("Slayers"),
    END("End"),
    DIANA("Diana"),
    MISC("Misc", listOf("General", "Inventory", "Hiders", "Tweaks")),
    DEBUG("Debug", listOf("Renderers", "Utils"));

    init {
        if (subcategories.isEmpty()) throw IllegalArgumentException("must provide at least 1 subcategory")
        if (subcategories.size > 6) throw IllegalArgumentException("limit of 6 subcategories")
    }

    companion object {
        fun byName(name: String) = entries.find { it.displayName == name }
    }
}