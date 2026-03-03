package com.github.synnerz.devonian.config.json

import com.github.synnerz.devonian.config.DataObject
import com.github.synnerz.devonian.utils.PersistentJson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive

class JsonDataObject(val json: JsonObject = JsonObject()) : DataObject() {
    override fun <T> set(key: String, value: T) = apply {
        when (value) {
            is Boolean -> json.addProperty(key, value)
            is String -> json.addProperty(key, value)
            is Char -> json.addProperty(key, value)
            is Number -> json.addProperty(key, value)
            is JsonArray -> json.add(key, value)
            is JsonObject -> json.add(key, value)
            is JsonDataObject -> json.add(key, value.json)
        }
    }

    override fun getString(key: String): String? = json.get(key)?.let {
            if ((it as? JsonPrimitive)?.isString == true) it.asString
            else null
        }
    override fun getInt(key: String): Int? = json.get(key)?.let {
            if ((it as? JsonPrimitive)?.isNumber == true) it.asInt
            else null
        }
    override fun getDouble(key: String): Double? = json.get(key)?.let {
            if ((it as? JsonPrimitive)?.isNumber == true) it.asDouble
            else null
        }
    override fun getFloat(key: String): Float? = json.get(key)?.let {
            if ((it as? JsonPrimitive)?.isNumber == true) it.asFloat
            else null
        }
        override fun getLong(key: String): Long? = json.get(key)?.let {
            if ((it as? JsonPrimitive)?.isNumber == true) it.asLong
            else null
        }
        override fun getBoolean(key: String): Boolean? = json.get(key)?.let {
            if ((it as? JsonPrimitive)?.isBoolean == true) it.asBoolean
            else null
        }
    override fun getList(key: String): List<*>? = json.get(key)?.let {
            (it as? JsonArray)?.asList()
        }
    override fun getMap(key: String): Map<*, *>? = json.get(key)?.let {
            (it as? JsonObject)?.asMap()
        }

    private val cached = mutableMapOf<String, JsonDataObject>()

    override fun getObject(key: String): DataObject = cached.getOrPut(key) {
        var obj = json.get(key)?.asJsonObject
        if (obj == null) {
            obj = JsonObject()
            json.add(key, obj)
        }
        return@getOrPut JsonDataObject(obj)
    }

    override fun toString(): String {
        return PersistentJson.gson.toJson(json)
    }
}