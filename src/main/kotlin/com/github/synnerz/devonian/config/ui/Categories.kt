package com.github.synnerz.devonian.config.ui

enum class Categories(
    val categoryName: String,
    val subcategories: List<String> = listOf("General"), // Remember limit is 6
    val displayName: String = categoryName
) {
    DUNGEONS("Dungeons", listOf("General", "Solvers")),
    DUNGEON_MAP("DungeonMap", listOf("General"), "Dungeon Map"),
    GARDEN("Garden"),
    SLAYERS("Slayers"),
    END("End"),
    DIANA("Diana"),
    MISC("Misc", listOf("General", "Inventory", "Hiders", "Tweaks"));

    companion object {
        fun byName(name: String) =
            Categories.entries.find { it.name == name.uppercase() }

        fun byDisplayName(name: String) =
            Categories.entries.find { it.displayName == name }
    }
}