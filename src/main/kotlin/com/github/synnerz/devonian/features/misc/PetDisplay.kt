package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.Location
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.StringUtils.colorCodes

object PetDisplay : TextHudFeature(
    "petDisplay",
    "Shows the currently equipped pet in hud.",
    subcategory = "General",
) {
    private const val KEY_NAME = "petDisplay"

    // https://regex101.com/r/cnWa1e/1
    private val equippedPetRegex = "^You summoned your ([\\w\\s]+)( .+?)?!$".toRegex()
    private val formattedEquippedPetRegex = "^§r§aYou summoned your §r((?:§.)*[\\w\\s]+)((?:§.)* .+?)?§r§a!$".toRegex()
    // no intellij {0,1}? cannot be simplified to ?
    private val formattedAutoPetRuleRegex = "^§cAutopet §eequipped your §7\\[Lvl \\d+] (?:.+? ){0,1}?((?:§.)*[\\w\\s]+)((?:§.)* .+?)?§e! §a§lVIEW RULE$".toRegex()
    private val despawnedPetRegex = "^You despawned your ([\\w\\s]+)( .+?)?!$".toRegex()
    private val formattedDespawnedPetRegex = "^§r§aYou despawned your §r((?:§.)*[\\w\\s]+)((?:§.)* .+?)?§r§a!$".toRegex()

    private var currentPet = BasicState("").also {
        it.listen { pet ->
            Scheduler.scheduleTask {
                setLine(pet)
            }
        }
    }

    override fun initialize() {
        Config.set(KEY_NAME, "")

        Config.onAfterLoad {
            currentPet.value = Config.get<String>(KEY_NAME) ?: ""
        }

        Config.onPreSave {
            Config.set(KEY_NAME, currentPet.value)
        }

        on<ChatEvent> { event ->
            if (equippedPetRegex.matches(event.message)) {
                val match = formattedEquippedPetRegex.matchEntire(event.text.colorCodes()) ?: return@on
                match.groupValues.getOrNull(1)?.let { currentPet.value = it }
                return@on
            }
            formattedAutoPetRuleRegex.matchEntire(event.text.string)?.let { match ->
                match.groupValues.getOrNull(1)?.let { currentPet.value = it }
            }
            if (despawnedPetRegex.matches(event.message)) {
                val match = formattedDespawnedPetRegex.matchEntire(event.text.colorCodes()) ?: return@on
                match.groupValues.getOrNull(1)?.let { currentPet.value = "" }
                return@on
            }
        }

        on<RenderOverlayEvent> {
            draw(it.ctx)
        }.setEnabled(
            Location.stateInSkyblock.zip(
                Location.stateInArea("the rift").map(Boolean::not),
                Boolean::and
            )
        )
    }

    override fun getEditText(): List<String> = listOf("&6Rat")
}