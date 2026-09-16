package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.WebRequests
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.PersistentJson
import com.github.synnerz.devonian.utils.PersistentJsonClass
import com.google.gson.reflect.TypeToken
import java.util.concurrent.ConcurrentHashMap

object DodgeList : Feature(
    "dodgeList",
    "Displays the reason a user was added to the dodge list if they join through party finder (do /dv dodge help)",
    Categories.DUNGEONS,
    subcategory = "QOL",
    searchTags = setOf("shitter")
) {
    private val partyFinderJoinRegex = "^Party Finder > (\\w{1,16}) joined the dungeon group! \\((?:Healer|Tank|Mage|Berserk|Archer) Level \\d+\\)$".toRegex()
    private val dodgePlayers = object : PersistentJsonClass<MutableMap</* UUID */String, DodgeData>>(
        "devonian/dodgelist.json",
        object : TypeToken<MutableMap<String, DodgeData>>() {}
    ) {
        override fun onLoadDefault() {
            data = mutableMapOf()
        }
    }
    private val uuidCache = ConcurrentHashMap<String, String>()

    data class DodgeData(
        val reason: String,
        val importedFrom: String? = null,
    )
    data class PlayerDBData(
        val code: String,
        val message: String,
        val data: Map<String, Map<String, Any>>,
        val success: Boolean,
    )

    override fun initialize() {
        // TODO: on initialize, reload those that have links
        dodgePlayers.load()

        DevonianCommand.command.subcommand("dodge", true) { _, args ->
            if (args.isEmpty()) {
                ChatUtils.sendMessage("&cDodgeList a valid MODE was not provided", true)
                return@subcommand 0
            }
            val mode = args.firstOrNull() as? String? ?: return@subcommand 0

            when (mode.lowercase()) {
                "add" -> {
                    val username = args.getOrNull(1) as? String?
                    val reason = args.getOrNull(2) as? String? ?: "Not Provided"
                    if (username.isNullOrEmpty()) {
                        ChatUtils.sendMessage("&cDodgeList &e$username&c is not a valid username", true)
                        return@subcommand 0
                    }

                    requestUUID(username) { uuid ->
                        if (dodgePlayers.data!!.containsKey(uuid)) {
                            ChatUtils.sendMessage("&c&DodgeList user with name &e$username&c is already in the list", true)
                            return@requestUUID
                        }

                        dodgePlayers.data!![uuid] = DodgeData(reason)
                        ChatUtils.sendMessage("&bDodgeList added user &a$username&b with reason &a$reason", true)
                    }
                }
                "remove" -> {
                    val username = args.getOrNull(1) as? String?
                    if (username.isNullOrEmpty()) {
                        ChatUtils.sendMessage("&cDodgeList &e$username&c is not a valid username", true)
                        return@subcommand 0
                    }

                    requestUUID(username) { uuid ->
                        dodgePlayers.data!!.remove(uuid) ?: return@requestUUID
                        ChatUtils.sendMessage("&cDodgeList removed user &e$username", true)
                    }
                }
                "clear" -> {
                    dodgePlayers.data!!.clear()
                    ChatUtils.sendMessage("&cDodgeList removed &eALL&c users", true)
                }
                "check" -> {
                    val username = args.getOrNull(1) as? String?
                    if (username.isNullOrEmpty()) {
                        ChatUtils.sendMessage("&cDodgeList &e$username&c is not a valid username", true)
                        return@subcommand 0
                    }

                    requestUUID(username) { uuid ->
                        val data = dodgePlayers.data!![uuid]
                        if (data == null) {
                            ChatUtils.sendMessage("&cDodgeList could not find user &e$username&c in list", true)
                            return@requestUUID
                        }

                        ChatUtils.sendMessage("&bDodgeList user &e$username&b is in the list with reason &e${data.reason}", true)
                    }
                }
                "import" -> {
                    val link = minecraft.keyboardHandler.clipboard
                    if (link.isEmpty()) {
                        ChatUtils.sendMessage("&cDodgeList &e$link&c is not a valid import link", true)
                        return@subcommand 0
                    }
                    if (!link.startsWith("https://raw.githubusercontent.com/")) {
                        ChatUtils.sendMessage("&cDodgeList link &e$link&c is not a valid import link", true)
                        return@subcommand 0
                    }

                    val loading = ChatUtils.literal("${ChatUtils.prefix} &bDodgeList loading...")
                    ChatUtils.sendMessage(loading)

                    loadImport(link) { data ->
                        ChatUtils.deleteMessage(loading)

                        dodgePlayers.data!!.entries.reversed().forEach { (k, v) ->
                            if (v.importedFrom != link.lowercase()) return@forEach
                            dodgePlayers.data!!.remove(k)
                        }

                        data.forEach { (k, v) ->
                            dodgePlayers.data!![k] = DodgeData(v, link.lowercase())
                        }

                        ChatUtils.sendMessage("&bDodgeList successfully imported &e${data.size}&b users to the list", true)
                    }
                }
                "help" -> {
                    ChatUtils.sendMessage("&bDodgeList creating or importing dodge lists", true)
                    ChatUtils.sendMessage("&aADD&f: &eadding a user is very simple, all you have to do is &6\"/dv dodge add <username> <reason>\"&e the reason can be empty")
                    ChatUtils.sendMessage("&cREMOVE&f: &eremoving users is equally as simple as adding them, simply do &6\"/dv dodge remove <username>\"&e and it's done")
                    ChatUtils.sendMessage("&4CLEAR&f: &eif you think your list is too much and want to clear it entirely in one go you can do &6\"/dv dodge clear\"")
                    ChatUtils.sendMessage("&bCHECK&f: &echecking whether a user is in the list and for what reason &6\"/dv dodge check <username>\"")
                    ChatUtils.sendMessage("&dIMPORT&f: &eimporting other people's public lists. for this the list has to be in a &5Github&e repository and you can click the &f\"raw\"&e button and copy the url then do &6\"/dv dodge import\" the link will be taken from your clipboard")
                }
                // TODO: maybe make a clearImport one with the link
            }
            1
        }
            .word("MODE")
            .suggest("MODE", *listOf(
                "ADD",
                "REMOVE",
                "CLEAR",
                "CHECK",
                "IMPORT",
                "HELP",
            ).toTypedArray())
            .word("other1")
            .greedyString("other2")

        on<ChatEvent> { event ->
            val ( username ) = event.matches(partyFinderJoinRegex) ?: return@on
            val playerName = minecraft.player?.name?.string ?: return@on
            if (username.equals(playerName, ignoreCase = true)) return@on

            // yes i could link it through hypixelmodapi but i am too lazy
            requestUUID(username) { playerUUID ->
                val reason = dodgePlayers.data!![playerUUID]?.reason ?: return@requestUUID
                ChatUtils.sendMessage("&cDodgeList found player &e$username&c for &e$reason", true)
            }
        }
    }

    private fun requestUUID(username: String, sendMessage: Boolean = true, successCb: (String) -> Unit) {
        val loading = ChatUtils.literal("${ChatUtils.prefix} &bDodgeList loading...")
        if (sendMessage)
            ChatUtils.sendMessage(loading)

        uuidCache[username.lowercase()]?.let {
            ChatUtils.deleteMessage(loading)
            successCb(it)
            return
        }

        WebRequests.withName("DodgeList") {
            val response = WebRequests.get("https://playerdb.co/api/player/minecraft/$username")
            if (response.isEmpty()) return@withName
            val data = PersistentJson.gson.fromJson(response, PlayerDBData::class.java)
            if (!data.success) {
                println("DodgeList failed to retrieve data for user $username with error message ${data.message}")
                return@withName
            }
            val playerUUID = data.data["player"]?.get("id") as? String?
            if (playerUUID == null) {
                println("DodgeList failed to find player UUID for $username")
                return@withName
            }

            uuidCache[username.lowercase()] = playerUUID
            Scheduler.scheduleTask {
                ChatUtils.deleteMessage(loading)
                successCb(playerUUID)
            }
        }
    }

    private fun loadImport(link: String, successCb: (Map<String, String>) -> Unit) {
        WebRequests.withName("DodgeList\$Import") {
            val response = WebRequests.get(link)
            if (response.isEmpty()) return@withName
            val data = PersistentJson.gson.fromJson(response, Map::class.java) as? Map<String, String> ?: return@withName

            Scheduler.scheduleTask { successCb(data) }
        }
    }
}