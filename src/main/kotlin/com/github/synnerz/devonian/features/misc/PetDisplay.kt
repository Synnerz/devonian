package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.StringUtils.colorCodes

object PetDisplay : TextHudFeature(
    "petDisplay",
    "Shows the currently equipped pet in hud.",
    subcategory = "General",
) {
    private const val KEY_NAME = "petDisplay"
    private val equippedPetRegex = "^You summoned your ([\\w ]+)(?: ✦)?!\$".toRegex()
    private val formattedEquppedRegex = "^§r§aYou summoned your §r([§\\w ]+)(?: [§✦]+)?§r§a!$".toRegex()
    private val autoPetRuleRegex = "^Autopet equipped your \\[Lvl \\d+](?: \\[\\d+.])? ([\\w ]+)(?: .)?! VIEW RULE$".toRegex()
    private val formattedAutoRuleRegex = "^§r§cAutopet §eequipped your §7\\[Lvl \\d+](?: [§\\w \\[\\]✦]+)? ([§\\w ]+)(?:§\\w .)?(?: . )?§e! §a§lVIEW RULE$".toRegex()
    private var currentPet: String? = ""

    override fun initialize() {
        Config.set(KEY_NAME, "")

        Config.onAfterLoad {
            val cached = Config.get<String>(KEY_NAME) ?: ""
            currentPet = cached
        }

        Config.onPreSave {
            currentPet ?: return@onPreSave
            Config.set(KEY_NAME, currentPet)
        }

        on<ChatEvent> { event ->
            event.matches(equippedPetRegex)?.let {
                val match = formattedEquppedRegex.matchEntire(event.text.colorCodes())?.groupValues?.drop(1) ?: return@on
                currentPet = match[0]
                return@on
            }

            event.matches(autoPetRuleRegex)?.let {
                val match = formattedAutoRuleRegex.matchEntire(event.text.colorCodes())?.groupValues?.drop(1) ?: return@on
                currentPet = match[0]
                return@on
            }
        }

        on<RenderOverlayEvent> {
            if (currentPet == null) return@on
            setLine(currentPet!!)
            draw(it.ctx)
        }
    }

    override fun getEditText(): List<String> = listOf("&6Rat")
}