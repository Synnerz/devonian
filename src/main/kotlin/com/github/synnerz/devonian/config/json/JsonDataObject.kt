package com.github.synnerz.devonian.config.json

import com.github.synnerz.devonian.config.DataObject
import com.github.synnerz.devonian.utils.PersistentJson
import com.google.gson.JsonArray
import com.google.gson.JsonObject

class JsonDataObject(private val json: JsonObject) : DataObject() {
    override fun <T> set(key: String, value: T) = apply {
        when (value) {
            is Boolean -> json.addProperty(key, value)
            is String -> json.addProperty(key, value)
            is Char -> json.addProperty(key, value)
            is Number -> json.addProperty(key, value)
            is JsonArray -> json.add(key, value)
            is JsonObject -> json.add(key, value)
        }
    }

    override fun getString(key: String): String? = json.get(key)?.asString
    override fun getInt(key: String): Int? = json.get(key)?.asInt
    override fun getDouble(key: String): Double? = json.get(key)?.asDouble
    override fun getFloat(key: String): Float? = json.get(key)?.asFloat
    override fun getLong(key: String): Long? = json.get(key)?.asLong
    override fun getBoolean(key: String): Boolean? = json.get(key)?.asBoolean
    override fun getList(key: String): List<*>? = json.get(key)?.asJsonArray?.toList()
    override fun getMap(key: String): Map<*, *>? = json.get(key)?.asJsonObject?.asMap()

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