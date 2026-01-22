package com.github.synnerz.devonian.features.dungeons.solvers

import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature

object PuzzleTimers : Feature(
    "puzzleTimers",
    "Displays how long it took you to complete the puzzle you were in.",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "Solvers",
    searchTags = setOf("complete"),
)