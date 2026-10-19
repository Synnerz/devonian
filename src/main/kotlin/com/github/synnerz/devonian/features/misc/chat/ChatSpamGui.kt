package com.github.synnerz.devonian.features.misc.chat

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.TickEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.hud.texthud.OverflowTextHud
import com.github.synnerz.devonian.hud.texthud.StylizedTextHud
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.OpenEditor
import com.github.synnerz.devonian.utils.StringUtils.colorCodes
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import java.util.EnumMap

object ChatSpamGui : TextHudFeature(
    "chatSpamGui",
    "Cancels messages and shows them in a separate gui.",
    Categories.MISC,
    subcategory = "Chat",
) {
    private var CONFIG_KEY = "chatSpamGui"

    private var isModifying = false
    private val SETTING_EDIT = addButton(
        {
            if (isModifying) return@addButton
            isModifying = true
            OpenEditor.edit(messages.mapNotNull { it.serialize() }) {
                messages = LinkedHashSet(it.map { Filter.deserialize(it) })
                rebuild = true
                isModifying = false
            }
        },
        description = "Will open up a new window.",
        displayName = "Edit Message List",
    )
    private val SETTING_IMPORT = addButton(
        {
            val count = messages.size
            messages = LinkedHashSet(messages).also {
                it.addAll(
                    CancelMessages.messagesList.map {
                        it.checkRegex()
                        return@map it.cachedRegex?.let { Filter(null, it) }
                            ?: Filter(it.message, null)
                    }
                )
            }
            ChatUtils.sendMessage("Imported ${messages.size - count} messages from CancelMessages")
            rebuild = true
        },
        description = "yes i am lazy how could you tell",
        displayName = "Import From CancelMessages",
    )

    @Volatile
    private var messages = emptySet<Filter>()
    @Volatile
    private var rebuild = true

    private var messageSet = emptySet<String>()
    private var regexList = emptyList<Regex>()

    private data class Filter(val str: String?, val reg: Regex?) {
        fun matches(input: String): Boolean {
            if (str != null) return str == input
            if (reg != null) return reg.matches(input)
            return false
        }

        fun serialize(): String? {
            if (str != null) return str
            if (reg != null) return "/${reg}/${reg.options.joinToString { regOption[it]?.toString() ?: "" }}"
            return null
        }

        companion object {
            private val regRegex = "^/(.+?)/(.*?)$".toRegex()

            private val regOption = EnumMap(
                mapOf(
                    RegexOption.IGNORE_CASE to 'i',
                    RegexOption.MULTILINE to 'm',
                    RegexOption.DOT_MATCHES_ALL to 's',
                    RegexOption.COMMENTS to 'x',
                    RegexOption.UNIX_LINES to 'd',
                )
            )
            private val regOptionR = regOption.entries.associate { it.value to it.key }

            fun deserialize(str: String): Filter {
                return regRegex.matchEntire(str)?.let {
                    Filter(
                        null,
                        Regex(
                            it.groupValues[1],
                            it.groupValues[2]
                                .toCharArray()
                                .map { regOptionR[it] }
                                .filterNotNullTo(mutableSetOf()),
                        ),
                    )
                } ?: Filter(str, null)
            }
        }
    }

    override fun createHud(): StylizedTextHud = OverflowTextHud(configName, this)

    override fun getEditText(): List<String> = listOf("lots of messages here", "trust")

    override fun initialize() {
        Config.set(CONFIG_KEY, JsonArray())

        Config.onAfterLoad {
            val data = Config.get<List<JsonElement>>(CONFIG_KEY) ?: return@onAfterLoad

            messages = LinkedHashSet(
                data.mapNotNull {
                    if (it !is JsonPrimitive) return@mapNotNull null
                    if (!it.isString) return@mapNotNull null
                    Filter.deserialize(it.asString)
                }
            )
        }

        on<ChatEvent> { event ->
            if (!messageSet.contains(event.message) && regexList.none { it.matches(event.message) }) return@on

            event.cancel()
            event.text.colorCodes().split('\n').forEach {
                addLine(it)
            }
        }

        on<TickEvent> {
            if (!rebuild) return@on

            rebuild = false
            messageSet = messages.mapNotNull { it.str }.toHashSet()
            regexList = messages.mapNotNull { it.reg }

            val arr = JsonArray()
            messages.forEach {
                it.serialize()?.let { arr.add(it) }
            }
            Config.set(CONFIG_KEY, arr)
        }

        on<RenderOverlayEvent> { event ->
            draw(event.ctx)
        }
    }
}