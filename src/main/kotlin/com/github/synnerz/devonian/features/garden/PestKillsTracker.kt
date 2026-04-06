package com.github.synnerz.devonian.features.garden

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.events.ClientThreadServerTickEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.garden.GardenEvents
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.PersistentJsonClass

object PestKillsTracker : TextHudFeature(
    "pestKillsTracker",
    "Tracks the pests you've killed (/dv rspestkilltracker to reset)",
    Categories.GARDEN,
    "garden"
) {
    private val loader = object : PersistentJsonClass<PestKillsData>("devonian/pestkillsdata.json", PestKillsData::class.java) {
        override fun onLoadDefault() {
            data = PestKillsData()
        }
    }

    data class PestKillsData(
        var fly: Int = 0,
        var cricket: Int = 0,
        var locust: Int = 0,
        var rat: Int = 0,
        var mosquito: Int = 0,
        var earthworm: Int = 0,
        var mite: Int = 0,
        var moth: Int = 0,
        var slug: Int = 0,
        var beetle: Int = 0,
        var dragonfly: Int = 0,
        var firefly: Int = 0,
        var prayingMantis: Int = 0,
    ) {
        fun total(): Int {
            return fly + cricket + locust + rat + mosquito + earthworm + mite + moth + slug + beetle + dragonfly + firefly + prayingMantis
        }
    }

    override fun initialize() {
        loader.load()
        DevonianCommand.command.subcommand("rspestkilltracker") { _, args ->
            loader.data?.apply {
                fly = 0
                cricket = 0
                locust = 0
                rat = 0
                mosquito = 0
                earthworm = 0
                mite = 0
                moth = 0
                slug = 0
                beetle = 0
                dragonfly = 0
                firefly = 0
                prayingMantis = 0
                ChatUtils.sendMessage("&aSuccessfully reset &bPestKillTracker&a data", true)
            }
            1
        }

        on<GardenEvents.PestKill> { event ->
            loader.data?.apply {
                when (event.name.trim()) {
                    "Fly" -> fly++
                    "Cricket" -> cricket++
                    "Locust" -> locust++
                    "Rat" -> rat++
                    "Mosquito" -> mosquito++
                    "Earthworm" -> earthworm++
                    "Mite" -> mite++
                    "Moth" -> moth++
                    "Slug" -> slug++
                    "Beetle" -> beetle++
                    "Dragonfly" -> dragonfly++
                    "Praying Mantis" -> prayingMantis++
                    "Firefly" -> firefly++
                }
            }
        }

        on<ClientThreadServerTickEvent> {
            loader.data?.apply {
                setLines(listOf(
                    "&aFly&f: &a&l${fly}",
                    "&aCricket&f: &a&l${cricket}",
                    "&aLocust&f: &a&l${locust}",
                    "&aRat&f: &a&l${rat}",
                    "&aMosquito&f: &a&l${mosquito}",
                    "&aEarthworm&f: &a&l${earthworm}",
                    "&aMite&f: &a&l${mite}",
                    "&aMoth&f: &a&l${moth}",
                    "&aSlug&f: &a&l${slug}",
                    "&aBeetle&f: &a&l${beetle}",
                    "&aDragonfly&f: &a&l${dragonfly}",
                    "&aPraying Mantis&f: &a&l${prayingMantis}",
                    "&aFirefly&f: &a&l${firefly}",
                    "&eTotal&f: &e&l${total()}"
                ))
            }
        }

        on<RenderOverlayEvent> {
            draw(it.ctx)
        }
    }

    override fun getEditText(): List<String> = listOf(
        "&aFly&f: &a&l1",
        "&aCricket&f: &a&l1",
        "&aLocust&f: &a&l1",
        "&aRat&f: &a&l1",
        "&aMosquito&f: &a&l1",
        "&aEarthworm&f: &a&l1",
        "&aMite&f: &a&l1",
        "&aMoth&f: &a&l1",
        "&aSlug&f: &a&l1",
        "&aBeetle&f: &a&l1",
        "&aDragonfly&f: &a&l1",
        "&aPraying Mantis&f: &a&l1",
        "&aFirefly&f: &a&l1",
        "&eTotal&f: &e&l1",
    )
}