package com.github.synnerz.devonian.utils

import com.github.synnerz.devonian.config.json.JsonDataObject
import com.github.synnerz.devonian.utils.StringUtils.colorCodes
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import net.minecraft.core.BlockPos
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.*
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f

object Serializer {
    fun serializeNBT(nbt: Tag): JsonElement? = when (nbt) {
        is EndTag -> null

        is StringTag -> JsonPrimitive(nbt.value)

        is ByteTag -> JsonPrimitive(nbt.value)
        is ShortTag -> JsonPrimitive(nbt.value)
        is IntTag -> JsonPrimitive(nbt.value)
        is LongTag -> JsonPrimitive(nbt.value)
        is FloatTag -> JsonPrimitive(nbt.value)
        is DoubleTag -> JsonPrimitive(nbt.value)

        is ByteArrayTag -> {
            val arr = nbt.asByteArray
            val json = JsonArray(nbt.size())
            arr.forEach { json.add(it) }
            json
        }

        is IntArrayTag -> {
            val arr = nbt.asIntArray
            val json = JsonArray(nbt.size())
            arr.forEach { json.add(it) }
            json
        }

        is LongArrayTag -> {
            val arr = nbt.asLongArray
            val json = JsonArray(nbt.size())
            arr.forEach { json.add(it) }
            json
        }

        is ListTag -> {
            val json = JsonArray(nbt.size)
            for (i in 0 until nbt.size) {
                json.add(serializeNBT(nbt[i]))
            }
            json
        }

        is CompoundTag -> {
            val json = JsonObject()
            nbt.forEach { k, v -> json.add(k, serializeNBT(v)) }
            json
        }
    }

    fun serializeItem(stack: ItemStack): JsonDataObject {
        val item = stack.item
        val obj = JsonDataObject()

        stack.get(DataComponents.CUSTOM_NAME)?.let {
            obj.set("name", it.colorCodes())
            obj.set("name_", it.string)
        }
        obj.set("id", BuiltInRegistries.ITEM.getKey(item).toString())
        obj.set("count", stack.count)
        obj.set("damage", stack.damageValue)

        stack.get(DataComponents.PROFILE)?.let {
            val data = it.partialProfile() ?: return@let
            val profile = obj.getObject("Profile")
            profile.set("id", data.id.toString())
            profile.set("name", data.name)
            data.properties.forEach { k, v ->
                val e = profile.getObject(k)
                e.set("name", v.name)
                e.set("value", v.value)
                e.set("signature", v.signature)
            }
        }

        stack.get(DataComponents.LORE)?.let {
            val lines = it.lines
            val arr = JsonArray(lines.size)
            val arrUnf = JsonArray(lines.size)
            lines.forEach {
                arr.add(it.colorCodes())
                arrUnf.add(it.string)
            }
            obj.set("Lore", arr)
            obj.set("Lore_", arrUnf)
        }

        stack.get(DataComponents.ENCHANTMENTS)?.let {
            if (it.isEmpty) return@let
            val ench = obj.getObject("Enchantments")
            it.entrySet().forEach {
                ench.set(it.key.registeredName, it.intValue)
            }
        }

        stack.get(DataComponents.STORED_ENCHANTMENTS)?.let {
            if (it.isEmpty) return@let
            val ench = obj.getObject("EnchantmentsStored")
            it.entrySet().forEach {
                ench.set(it.key.registeredName, it.intValue)
            }
        }

        stack.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE)?.let {
            obj.set("forceEnchanted", it)
        }

        stack.get(DataComponents.CUSTOM_DATA)?.let {
            val nbt = it.copyTag()
            obj.set("ExtraAttributes", serializeNBT(nbt))
        }

        return obj
    }

    fun serializeBlockPos(pos: BlockPos): JsonDataObject {
        val obj = JsonDataObject()
        obj.set("x", pos.x)
        obj.set("y", pos.y)
        obj.set("z", pos.z)
        return obj
    }

    fun serializeBlockState(pos: BlockState): JsonDataObject {
        val obj = JsonDataObject()
        obj.set("block", BuiltInRegistries.BLOCK.getKey(pos.block).toString())
        return obj
    }

    fun serializeVec(pos: Vec3): JsonDataObject {
        val obj = JsonDataObject()
        obj.set("x", pos.x)
        obj.set("y", pos.y)
        obj.set("z", pos.z)
        return obj
    }

    fun serializeVector3f(pos: Vector3f): JsonDataObject {
        val obj = JsonDataObject()
        obj.set("x", pos.x)
        obj.set("y", pos.y)
        obj.set("z", pos.z)
        return obj
    }

    fun serializeEntity(ent: Entity): JsonDataObject {
        val obj = JsonDataObject()
        obj.set("id", ent.id)
        obj.set("uuid", ent.uuid.toString())
        obj.set("type", EntityType.getKey(ent.type).toString())

        val name = JsonDataObject()
        ent.customName?.let {
            name.set("customName", it.colorCodes())
            name.set("customName_", it.string)
        }
        ent.displayName?.let {
            name.set("displayName", it.colorCodes())
            name.set("displayName_", it.string)
        }
        obj.set("name", name)

        val pos = JsonDataObject()
        pos.set("x", ent.x)
        pos.set("y", ent.y)
        pos.set("z", ent.z)
        pos.set("xRot", ent.xRot)
        pos.set("yRot", ent.yRot)
        obj.set("pos", pos)

        if (ent is LivingEntity) {
            val hp = JsonDataObject()
            hp.set("health", ent.health)
            hp.set("max", ent.maxHealth)
            obj.set("hp", hp)

            val eq = JsonDataObject()
            EquipmentSlot.entries.forEach { slot ->
                val item = ent.getItemBySlot(slot) ?: return@forEach
                if (item.isEmpty) return@forEach
                eq.set(slot.name, serializeItem(item))
            }
            obj.set("equipment", eq)
        }

        if (ent is Player) {
            name.set("playerName", ent.gameProfile.name)
            obj.set("profile", ent.gameProfile.id.toString())
        }

        if (ent is ItemEntity) {
            obj.set("item", serializeItem(ent.item))
        }

        return obj
    }

    fun serialize(value: Any?): JsonDataObject {
        return when (value) {
            is ItemStack -> serializeItem(value)
            is BlockPos -> serializeBlockPos(value)
            is BlockState -> serializeBlockState(value)
            is Vec3 -> serializeVec(value)
            is Vector3f -> serializeVector3f(value)
            is Entity -> serializeEntity(value)
            null -> JsonDataObject()
            else -> {
                val obj = JsonDataObject()
                obj.set("class", value.javaClass.name)
                when (value) {
                    is Enum<*> -> obj.set("name", value.name)
                    is Boolean -> obj.set("value", value)
                    is Number -> obj.set("value", value)
                    is String -> obj.set("value", value)
                }
                obj
            }
        }
    }
}