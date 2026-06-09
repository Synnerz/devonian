package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.Location
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.ClientContainerCloseEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.ServerContainerCloseEvent
import com.github.synnerz.devonian.api.events.ServerContainerOpenEvent
import com.github.synnerz.devonian.api.events.ServerContainerSetSlotEvent
import com.github.synnerz.devonian.api.events.TabUpdateEvent
import com.github.synnerz.devonian.api.events.TickEvent
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.config.DataObject
import com.github.synnerz.devonian.config.json.JsonDataObject
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.BoundingBox
import com.github.synnerz.devonian.utils.StringUtils.colorCodes
import com.google.common.collect.HashMultimap
import com.google.common.collect.ImmutableMultimap
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.Property
import com.mojang.authlib.properties.PropertyMap
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.ResolvableProfile
import java.util.UUID

object PetDisplay : TextHudFeature(
    "petDisplay",
    "Shows the currently equipped pet in hud.",
    subcategory = "General",
) {
    private val SETTING_ICON = addSwitch(
        "icon",
        true,
        "Renders the pet next to the hud element.",
        "Render Pet",
    )
    private val SETTING_LEVEL = addSwitch(
        "level",
        true,
        "Include the pet's level in the hud.",
        "Show Pet Level",
    )
    private val SETTING_COSMETIC_LEVEL = addSwitch(
        "cosmeticLevel",
        true,
        "Include the pet's (golden dragon's) cosmetic level in the hud.",
        "Show Cosmetic Level",
    )
    private val SETTING_SKINNED = addSwitch(
        "skinned",
        true,
        "Include the skinned icon (✦) in the hud.",
        "Show Skinned Icon",
    )

    private const val CURRENT_KEY_NAME = "petDisplayCurrent2"
    private const val SAVED_KEY_NAME = "petDisplaySaved2"

    // https://regex101.com/r/yLlhXf/1
    // 1: pet name, 2: skin
    private val equippedPetRegex = "^You summoned your ([\\w\\s]+)( ✦)?!$".toRegex()
    // 1: color code, 2: pet name, 3: skin
    private val formattedEquippedPetRegex = "^§r§aYou summoned your §r((?:§.)*)([\\w\\s]+)(?:§r((?:§.)* ✦))?§r§a!$".toRegex()
    // 1: level, 2: cosmetic level, 3: color code, 4: pet name, 5: skin
    private val autoPetRuleRegex = "^§cAutopet §eequipped your §7\\[Lvl (\\d+)](?: §8\\[§\\w([\\dkmb.,]+)§?8?§\\w[✦⚔]§8])? ((?:§.)*)([\\w\\s]+)((?:§.)* (?:§.)*✦)?§e! §a§lVIEW RULE$".toRegex()
    // 1: pet name, 2: skin
    private val despawnedPetRegex = "^You despawned your ([\\w\\s]+)( ✦)?!$".toRegex()
    // 1: color code, 2: pet name, 3: skin
    // private val formattedDespawnedPetRegex = "^§r§aYou despawned your §r((?:§.)*)([\w\s]+)(?:§r((?:§.)* ✦))?§r§a!$".toRegex()
    // 1: level, 2: cosmetic level, 3: pet name, 4: skin
    private val tabPetRegex = "^ \\[Lvl (\\d+)](?: \\[(\\d+)✦])? ([\\w\\s]+)( ✦)?$".toRegex()
    // 1: level, 2: cosmetic level, 3: color code, 4: pet name, 5: skin
    private val formattedTabPetRegex = "^§r §r§7\\[Lvl (\\d+)](?: §r§8\\[§r§6(\\d+)§r§4✦§r§8])? §r((?:§.)*)([\\w\\s]+)(?:§r((?:§.)* ✦))?$".toRegex()

    // https://regex101.com/r/f9xwqQ/3
    private val petsMenuRegex = "^(\\(\\d+/\\d+\\) )?Pets$".toRegex()
    // 1: level, 2: cosmetic level, 3: pet name, 4: skin
    private val petsMenuNameRegex = "^(?:⭐ )?\\[Lvl (\\d+)](?: \\[(\\d+)✦])? ([\\w\\s]+)( ✦)?$".toRegex()
    // 1: level, 2: cosmetic level, 3: color code, 4: pet name, 5: skin
    private val formattedPetsMenuNameRegex = "^(?:§r§e⭐ )?§r§7\\[Lvl (\\d+)](?: §r§8\\[§r§6(\\d+)§r§4✦§r§8])? §r((?:§.)*)([\\w\\s]+)(?:§r((?:§.)* ✦))?$".toRegex()

    private data class Pet(
        val name: String,
        val skinned: String,
        val colorCode: String,

        val level: Int = -1,
        val cosmeticLevel: Int = -1,
    ) {
        var page = 0
        var skin: Skin? = null
        // var sortOv = hashCode()

        fun serialize(): JsonDataObject? {
            if (page == 0) return null
            val obj = JsonDataObject()
            obj.set("name", name)
            obj.set("skinned", skinned)
            obj.set("colorCode", colorCode)
            obj.set("page", page)
            if (level != -1) obj.set("level", level)
            if (cosmeticLevel != -1) obj.set("cosmeticLevel", cosmeticLevel)
            skin?.let {
                val o = obj.getObject("skin")
                o.set("id", it.id.toString())
                o.set("value", it.value)
                o.set("signature", it.signature)
            }
            return obj
        }

        companion object {
            fun from(obj: DataObject): Pet? {
                val name = obj.get<String>("name") ?: return null
                val skinned = obj.get<String>("skinned") ?: return null
                val colorCode = obj.get<String>("colorCode") ?: return null
                val page = obj.get<Int>("page") ?: return null
                val level = obj.get<Int>("level")
                val cosmeticLevel = obj.get<Int>("cosmeticLevel")
                val skin = obj.getObject("skin").let {
                    val id = it.get<String>("id")?.let { UUID.fromString(it) } ?: return@let null
                    val value = it.get<String>("value") ?: return@let null
                    val signature = it.get<String>("signature") ?: return@let null
                    Skin(id, value, signature)
                }
                return Pet(name, skinned, colorCode, level ?: -1, cosmeticLevel ?: -1).also {
                    it.page = page
                    it.skin = skin
                }
            }

            // fun searchable(page: Int, start: Boolean) =
            //     Pet("i am not in the map", false, "surely not").also {
            //         it.page = page
            //         it.sortOv = if (start) Int.MIN_VALUE else Int.MAX_VALUE
            //     }
        }
    }

    private data class Skin(
        val id: UUID,
        val value: String,
        val signature: String,
    ) {
        val profile = GameProfile(
            id,
            "",
            PropertyMap(
                ImmutableMultimap.of(
                    "textures",
                    Property("textures", value, signature),
                )
            ),
        )
    }

    private class PetKeyView(val del: Pet) {
        override fun hashCode(): Int {
            return del.name.hashCode() xor del.skinned.hashCode() xor del.colorCode.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as PetKeyView

            if (del.name != other.del.name) return false
            if (del.skinned != other.del.skinned) return false
            if (del.colorCode != other.del.colorCode) return false

            return true
        }
    }

    private var currentPet: Pet? = null
    private var allPets = mutableMapOf<Pet, Pet>()
    private var reducedPets = HashMultimap.create<PetKeyView, Pet>()
    private var fakeItem: ItemStack? = null
    private var lastSkin: Skin? = null
    private var petMenuPage = -1

    override fun getBounds(): BoundingBox {
        val bounds = super.getBounds()
        if (!SETTING_ICON.get()) return bounds
        val d = 20.0 * scale
        return BoundingBox(
            bounds.x - d,
            bounds.y + (bounds.h - d) * 0.5,
            bounds.w + d,
            d
        )
    }

    override fun initialize() {
        Config.onAfterLoad {
            Config.getObject(CURRENT_KEY_NAME).let {
                currentPet = Pet.from(it)
            }

            allPets.clear()
            Config.get<List<JsonObject>>(SAVED_KEY_NAME)?.let { arr ->
                arr.forEach { obj ->
                    Pet.from(JsonDataObject(obj))?.let {
                        allPets[it] = it
                        reducedPets.put(PetKeyView(it), it)
                    }
                }
            }
        }

        Config.onPreSave {
            currentPet?.serialize()?.let {
                Config.set(CURRENT_KEY_NAME, it)
            }

            val arr = JsonArray()
            allPets.forEach { (_, p) ->
                p.serialize()?.let {
                    arr.add(it.json)
                }
            }
            Config.set(SAVED_KEY_NAME, arr)
        }

        on<ChatEvent> { event ->
            autoPetRuleRegex.matchEntire(event.text.string)?.let { match ->
                val levelS = match.groupValues.getOrNull(1) ?: return@on
                val level = levelS.toIntOrNull() ?: -1
                val cosmeticLevelS = match.groupValues.getOrNull(2) ?: return@on
                val cosmeticLevel = if (cosmeticLevelS.isEmpty()) -1 else cosmeticLevelS.toIntOrNull() ?: -1
                val colorCode = match.groupValues.getOrNull(3) ?: return@on
                val petName = match.groupValues.getOrNull(4) ?: return@on
                val skinned = match.groupValues.getOrNull(5) ?: return@on

                val pet = Pet(petName, skinned, colorCode, level, cosmeticLevel)
                currentPet = allPets.getOrElse(pet) { reducedPets.get(PetKeyView(pet)).firstOrNull() ?: pet }
                lastSkin = null

                return@on
            }
            if (equippedPetRegex.matches(event.message)) {
                val match = formattedEquippedPetRegex.matchEntire(event.text.colorCodes()) ?: return@on
                val colorCode = match.groupValues.getOrNull(1) ?: return@on
                val petName = match.groupValues.getOrNull(2) ?: return@on
                val skinned = match.groupValues.getOrNull(3) ?: return@on

                val pet = Pet(petName, skinned, colorCode)
                val arr = reducedPets.get(PetKeyView(pet))
                currentPet = arr.find { it != pet } ?: arr.firstOrNull() ?: pet
                lastSkin = null

                return@on
            }
            if (despawnedPetRegex.matches(event.message)) {
                currentPet = null
                lastSkin = null
                return@on
            }
        }

        on<TabUpdateEvent> { event ->
            if (!tabPetRegex.matches(event.message)) return@on
            val match = formattedTabPetRegex.matchEntire(event.comp.colorCodes()) ?: return@on
            val levelS = match.groupValues.getOrNull(1) ?: return@on
            val level = levelS.toIntOrNull() ?: -1
            val cosmeticLevelS = match.groupValues.getOrNull(2) ?: return@on
            val cosmeticLevel = if (cosmeticLevelS.isEmpty()) -1 else cosmeticLevelS.toIntOrNull() ?: -1
            val colorCode = match.groupValues.getOrNull(3) ?: return@on
            val petName = match.groupValues.getOrNull(4) ?: return@on
            val skinned = match.groupValues.getOrNull(5) ?: return@on

            val pet = Pet(petName, skinned, colorCode, level, cosmeticLevel)
            Scheduler.scheduleTask {
                currentPet = allPets.getOrElse(pet) { reducedPets.get(PetKeyView(pet)).firstOrNull() ?: pet }
                lastSkin = null
            }
        }

        on<ServerContainerOpenEvent> { event ->
            petMenuPage = -1
            val match = petsMenuRegex.matchEntire(event.titleStr) ?: return@on

            val page = match.groupValues.getOrNull(1)?.toIntOrNull() ?: return@on
            val max = match.groupValues.getOrNull(2)?.toIntOrNull() ?: return@on

            petMenuPage = page
            // val l = Pet.searchable(page, true)
            // val r = Pet.searchable(page, false)
            // val e = Pet.searchable(max, false)

            Scheduler.scheduleTask {
                // allPets.headMap(Pet.searchable(0, false), true).clear()
                // allPets.subMap(l, true, r, true).clear()
                // allPets.tailMap(e, false).clear()
                allPets.entries.removeIf {
                    val p = it.value.page
                    return@removeIf p !in 1 .. max || p == page
                }
                reducedPets.entries().removeIf {
                    val p = it.value.page
                    return@removeIf p !in 1 .. max || p == page
                }
                if (currentPet != null && currentPet !in allPets) currentPet = null
            }
        }

        on<ServerContainerSetSlotEvent> { event ->
            if (petMenuPage == -1) return@on

            if (event.slot == 4 && event.itemStack.item !== Items.BONE) {
                petMenuPage = -1
                return@on
            }

            if (event.slot / 9 !in 1 .. 4 || event.slot % 9 !in 1 .. 7) return@on
            if (event.itemStack.item !== Items.PLAYER_HEAD) return@on

            val itemName = event.itemStack.get(DataComponents.CUSTOM_NAME) ?: return@on
            if (!petsMenuNameRegex.matches(itemName.string)) return@on
            val match = formattedPetsMenuNameRegex.matchEntire(itemName.colorCodes()) ?: return@on

            val levelS = match.groupValues.getOrNull(1) ?: return@on
            val level = levelS.toIntOrNull() ?: -1
            val cosmeticLevelS = match.groupValues.getOrNull(2) ?: return@on
            val cosmeticLevel = if (cosmeticLevelS.isEmpty()) -1 else cosmeticLevelS.toIntOrNull() ?: -1
            val colorCode = match.groupValues.getOrNull(3) ?: return@on
            val petName = match.groupValues.getOrNull(4) ?: return@on
            val skinned = match.groupValues.getOrNull(5) ?: return@on

            val pet = Pet(petName, skinned, colorCode, level, cosmeticLevel)
            pet.page = petMenuPage
            val isSelected = ItemUtils.lore(event.itemStack)?.contains("Click to despawn!") ?: false

            val profile = event.itemStack.get(DataComponents.PROFILE)
            val skin = profile?.let {
                val tex = it.partialProfile().properties.get("textures").firstOrNull() ?: return@let null
                Skin(it.partialProfile().id, tex.value, tex.signature ?: "")
            }
            pet.skin = skin

            Scheduler.scheduleTask {
                allPets[pet] = pet
                reducedPets.put(PetKeyView(pet), pet)
                if (isSelected) currentPet = pet
            }
        }

        on<ServerContainerCloseEvent> {
            petMenuPage = -1
        }

        on<ClientContainerCloseEvent> {
            petMenuPage = -1
        }

        on<TickEvent> {
            val p = currentPet ?: return@on

            setLine(
                buildString {
                    if (SETTING_LEVEL.get() && p.level != -1) append("§a[Lvl ${p.level}] ")
                    if (SETTING_COSMETIC_LEVEL.get() && p.cosmeticLevel != -1) append("§b[${p.cosmeticLevel}✦] ")
                    append(p.colorCode)
                    append(p.name)
                    if (SETTING_SKINNED.get()) append(p.skinned)
                }
            )

            if (!SETTING_ICON.get()) return@on

            val skin = p.skin ?: return@on
            if (lastSkin == skin) return@on
            val prof = ResolvableProfile.createResolved(skin.profile)

            fakeItem = ItemStack(Items.PLAYER_HEAD, 1).also {
                it.set(DataComponents.PROFILE, prof)
            }
            lastSkin = skin
        }

        on<RenderOverlayEvent> { event ->
            if (currentPet == null) return@on

            draw(event.ctx)

            if (!SETTING_ICON.get()) return@on
            if (lastSkin == null) return@on

            val item = fakeItem ?: return@on
            val bounds = getBounds()

            event.ctx.pose()
                .pushMatrix()
                .translate(bounds.x.toFloat(), bounds.y.toFloat())
                .scale(1.25f * scale)

            event.ctx.fakeItem(item, 0, 0)

            event.ctx.pose().popMatrix()
        }.setEnabled(
            Location.stateInSkyblock.zip(
                Location.stateInArea("the rift").map(Boolean::not),
                Boolean::and
            )
        )
    }

    override fun getEditText(): List<String> = listOf("&6Rat")
}