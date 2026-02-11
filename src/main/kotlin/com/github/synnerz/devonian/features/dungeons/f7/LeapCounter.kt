package com.github.synnerz.devonian.features.dungeons.f7

import com.github.synnerz.devonian.api.dungeon.DungeonPlayer
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.TickEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.Alert
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.StringUtils

object LeapCounter : TextHudFeature(
    "leapCounter",
    "Displays a counter for how many people have leaped to you",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "F7"
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.F7.isActiveState)
    }

    private val SETTING_SHOW_ALERT = addSwitch(
        "showAlert",
        true,
        "Shows an alert whenever the expected amount of players have leaped to the standing spot",
        "LeapCounter Alert"
    )
    private val SETTING_PLAY_SOUND = addSwitch(
        "playSound",
        true,
        "Plays a sound whenever the expected amount of players have leaped to the standing spot",
        "LeapCounter Sound"
    )
    private val leapedToRegex = "^You have teleported to \\w{1,16}!$".toRegex()
    private val leapPositions = listOf(
        LeapPosition(58.5, 109.0, 131.5, 1.5),
        LeapPosition(60.5, 132.0, 139.0, 1.5),
        LeapPosition(69.5, 109.0, 122.5, 1.0),
        LeapPosition(48.5, 109.0, 122.5, 1.0),
        LeapPosition(54.5, 115.0, 50.5, 0.5),
        LeapPosition(2.5, 109.0, 104.5, 3.0, 3),
        LeapPosition(18.5, 121.5, 92.0, 2.0, 3),
        LeapPosition(54.5, 5.0, 76.5, 8.0),
    )
    private val leapedPlayers = mutableListOf<DungeonPlayer>()
    private var rolePositions: MutableList<LeapPosition> = mutableListOf()
    private var lastLeap = -1L

    data class LeapPosition(
        val x: Double,
        val y: Double,
        val z: Double,
        val dist: Double,
        val playerCount: Int = 4,
        var triggered: Boolean = false,
    )

    override fun initialize() {
        on<ChatEvent> { event ->
            if (event.matches(leapedToRegex) == null) return@on

            lastLeap = System.currentTimeMillis()
        }

        on<TickEvent> { event ->
            if (rolePositions.isEmpty()) {
                rolePositions.addAll(leapPositions)
                return@on
            }

            val player = minecraft.player ?: return@on
            var pos: LeapPosition? = null
            if (lastLeap != -1L && System.currentTimeMillis() - lastLeap < 3000) return@on

            rolePositions.forEach {
                if (player.distanceToSqr(it.x, it.y, it.z) > it.dist) return@forEach
                pos = it
            }

            if (pos == null || pos!!.triggered) {
                leapedPlayers.clear()
                clearLines()
                return@on
            }

            Dungeons.players.forEach { (k, v) ->
                val entity = v.entity ?: return@forEach
                if (entity.id == player.id) return@forEach

                val dist = entity.distanceToSqr(pos!!.x, pos!!.y, pos!!.z)

                if (dist > pos!!.dist) return@forEach
                if (leapedPlayers.contains(v)) return@forEach

                leapedPlayers.add(v)
            }
            if (leapedPlayers.isEmpty()) {
                clearLines()
                return@on
            }

            if (leapedPlayers.size >= pos!!.playerCount) {
                pos!!.triggered = true
                if (SETTING_SHOW_ALERT.get())
                    Alert.show("&9${pos!!.playerCount} have leaped", 1000, SETTING_PLAY_SOUND.get())
            }
            setLine("${StringUtils.colorForNumber(leapedPlayers.size, pos!!.playerCount)}${leapedPlayers.size}&f/&94 Leaped")
        }

        on<RenderOverlayEvent> {
            draw(it.ctx)
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        rolePositions.clear()
        lastLeap = -1L
    }

    override fun getEditText(): List<String> = listOf("${StringUtils.colorForNumber(2, 4)}2&f/&94 Leaped")
}