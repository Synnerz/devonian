package com.github.synnerz.devonian.features

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.WebRequests
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.utils.Toggleable
import com.google.gson.JsonParser
import kotlinx.coroutines.launch
import net.minecraft.SharedConstants
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import java.net.URI
import java.time.Instant
import kotlin.math.max

object CheckForUpdates : Feature(
    "checkForUpdates",
    "Automatically checks for updates, does not automatically update.",
    Categories.GLOBAL,
    subcategory = "Mod",
) {
    init {
        // default on
        configSwitch.value = true
    }

    private val SETTING_MODRINTH = addSwitch(
        "modrinth",
        true,
        "",
        "Check Modrinth",
    )
    private val SETTING_GITHUB_ACTIONS = addSwitch(
        "githubActions",
        false,
        "Checks for updates as soon as they are available. " +
        "Note: these are usually updated multiple times a day and are often unstable. " +
        "This also assumes you are on the latest available minecraft version we support.",
        "Check Github Actions",
    )

    private var checked = false

    private fun showUpdateMessage(url: String) {
        Scheduler.scheduleTask(100) {
            val txtRend = minecraft.font

            val lineBreak = "-".repeat(40)
            val breakWidth = txtRend.width(lineBreak)
            val spaceWidth = txtRend.width(" ")

            fun center(s: String): String {
                val w = txtRend.width(lineBreak)
                val o = (breakWidth - w) / 2
                return " ".repeat(max(0, o / spaceWidth)) + s
            }
            ChatUtils.sendMessage("§7$lineBreak")
            ChatUtils.sendMessage(center("§bDevonian Update!!!"))
            ChatUtils.sendMessage(
                Component.literal(center("§e§lCLICK HERE§r§a to view"))
                    .withStyle(Style.EMPTY.withClickEvent(ClickEvent.OpenUrl(URI.create(url))))
            )
            ChatUtils.sendMessage("§7$lineBreak")
        }
    }

    private suspend fun checkModrinth(): Boolean {
        val body = WebRequests.get(
            "https://api.modrinth.com/v2/project/j4Tr5Ve2/version?loaders=fabric&game_versions=[%22${
                SharedConstants.getCurrentVersion().id()
            }%22]&include_changelog=false"
        )

        val obj = JsonParser.parseString(body).asJsonArray
        if (obj.isEmpty) return false

        val latest = obj.first().asJsonObject

        val timeStr = latest["date_published"].asString
        val time = Instant.parse(timeStr)

        if (time >= Devonian.GIT_COMMIT_TIME) return false

        val versionId = latest["id"].asString
        val updateUrl = "https://modrinth.com/mod/j4Tr5Ve2/version/$versionId"
        showUpdateMessage(updateUrl)

        return true
    }

    private suspend fun checkGithub(): Boolean {
        // surely the most recent one isn't failing :pray:
        val body = WebRequests.get("https://api.github.com/repos/synnerz/devonian/actions/runs?per_page=1")

        val obj = JsonParser.parseString(body).asJsonObject

        val latest = obj["workflow_runs"].asJsonArray.first().asJsonObject

        val hash = latest["head_sha"].asString
        if (hash == Devonian.GIT_COMMIT_HASH) return false

        val runId = latest["id"].asLong
        val updateUrl = "https://github.com/Synnerz/devonian/actions/runs/$runId"
        showUpdateMessage(updateUrl)

        return true
    }

    fun postInitialize() {
        if (!isEnabled()) return
        if (checked) return
        checked = true

        if (Devonian.IS_LOCAL_BUILD) return
        WebRequests.ioScope.launch {
            try {
                if (SETTING_GITHUB_ACTIONS.get() && checkGithub()) return@launch
            } catch (_: Exception) {
            }
            try {
                if (SETTING_MODRINTH.get() && checkModrinth()) return@launch
            } catch (_: Exception) {
            }
        }
    }

    override fun initialize() {
        // only triggers if the feature is disabled on launch, then reenabled later
        children.add(
            object : Toggleable() {
                override fun add() = postInitialize()
                override fun remove() {}
            }
        )
    }
}