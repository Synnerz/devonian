package com.github.synnerz.devonian.features.garden

import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.TabUpdateEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.hud.texthud.TextHudFeature

object GardenDisplay : TextHudFeature(
    "gardenDisplay",
    "Displays all your Garden's current stats from tab. (for example current composter Fuel)",
    "garden",
    "garden"
) {
    private val nextVisitorRegex = "^ Next Visitor: ([\\w !]+)$".toRegex()
    private val totalVisitorsRegex = "^Visitors: \\((\\d+)\\)$".toRegex()
    private val jacobContestCdRegex = "^Jacob's Contest:(?: ([\\w ]+) left)?$".toRegex()
    private val jacobContestRegex = "^ Starts In: ([\\w ]+)$".toRegex()
    private val organicMatterRegex = "^ Organic Matter: ([\\w,.]+)$".toRegex()
    private val fuelRegex = "^ Fuel: ([\\w,.]+)$".toRegex()
    private val timeLeftRegex = "^ Time Left: ([\\w ]+)$".toRegex()
    private val storedCompostRegex = "^ Stored Compost: ([\\w,. ]+)$".toRegex()
    private var nextVisitor = "unknown"
    private var totalVisitors = "0"
    private var jacobContest = "unknown"
    private var jacobContestCd = "0"
    private var organicMatter = "0"
    private var fuel = "0"
    private var timeLeft = "unknown"
    private var storedCompost = "0"

    override fun initialize() {
        on<TabUpdateEvent> { event ->
            val nextVisitorMatch = event.matches(nextVisitorRegex)
            if (nextVisitorMatch != null) {
                nextVisitor = nextVisitorMatch[0]
                return@on
            }

            val totalVisitorsMatch = event.matches(totalVisitorsRegex)
            if (totalVisitorsMatch != null) {
                totalVisitors = totalVisitorsMatch[0]
                return@on
            }

            val jacobContestMatch = event.matches(jacobContestRegex)
            if (jacobContestMatch != null) {
                jacobContest = jacobContestMatch[0]
                return@on
            }

            val jacobContestCdMatch = event.matches(jacobContestCdRegex)
            if (jacobContestCdMatch != null) {
                val jacobCooldown = jacobContestCdMatch[0]
                jacobContestCd = if (jacobCooldown.trim().isEmpty()) "0" else jacobCooldown
                return@on
            }

            val organicMatterMatch = event.matches(organicMatterRegex)
            if (organicMatterMatch != null) {
                organicMatter = organicMatterMatch[0]
                return@on
            }

            val fuelMatch = event.matches(fuelRegex)
            if (fuelMatch != null) {
                fuel = fuelMatch[0]
                return@on
            }

            val timeLeftMatch = event.matches(timeLeftRegex)
            if (timeLeftMatch != null) {
                timeLeft = timeLeftMatch[0]
                return@on
            }

            val storedCompostMatch = event.matches(storedCompostRegex) ?: return@on

            storedCompost = storedCompostMatch[0]
        }

        on<WorldChangeEvent> {
            nextVisitor = "unknown"
            totalVisitors = "0"
            jacobContest = "unknown"
            organicMatter = "0"
            fuel = "0"
            timeLeft = "unknown"
            storedCompost = "0"
        }

        on<RenderOverlayEvent> { event ->
            setLines(
                listOf(
                    "&a&lGarden Display",
                    "&aVisitor in&f: &b$nextVisitor &f(&b$totalVisitors&f)",
                    "&aJacob's contest in&f: &6$jacobContest &7($jacobContestCd)",
                    "&eOrganic matter&f: &6$organicMatter",
                    "&9Fuel&f: &6$fuel",
                    "&aTime Left&f: &b$timeLeft",
                    "&aStored Compost&f: &6$storedCompost",
                )
            )
            draw(event.ctx)
        }
    }

    override fun getEditText(): List<String> = listOf(
        "&a&lGarden Display",
        "&aVisitor in&f: &b1m 10s",
        "&aJacob's contest in&f: &69m",
        "&eOrganic matter&f: &6100k",
        "&9Fuel&f: &6100k",
        "&aTime Left&f: &b1m 10s",
        "&aStored Compost&f: &6100",
    )
}