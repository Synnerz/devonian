package com.github.synnerz.devonian.config

abstract class DataObject {
    /**
     * - Sets a value to the specified name key
     */
    abstract fun <T> set(key: String, value: T): DataObject

    abstract fun getString(key: String): String?
    abstract fun getInt(key: String): Int?
    abstract fun getDouble(key: String): Double?
    abstract fun getFloat(key: String): Float?
    abstract fun getLong(key: String): Long?
    abstract fun getBoolean(key: String): Boolean?
    abstract fun getList(key: String): List<*>?
    abstract fun getMap(key: String): Map<*, *>?

    /**
     * - Gets a value from the specified name key
     */
    inline fun <reified T> get(key: String): T? = when (T::class) {
        String::class -> getString(key) as T?
        Int::class -> getInt(key) as T?
        Double::class -> getDouble(key) as T?
        Float::class -> getFloat(key) as T?
        Long::class -> getLong(key) as T?
        Boolean::class -> getBoolean(key) as T?
        List::class -> getList(key) as T?
        Map::class -> getMap(key) as T?
        else -> null
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String, type: T): T? = when (type) {
        is String -> getString(key)
        is Int -> getInt(key)
        is Double -> getDouble(key)
        is Float -> getFloat(key)
        is Long -> getLong(key)
        is Boolean -> getBoolean(key)
        is List<*> -> getList(key)
        is Map<*, *> -> getMap(key)
        else -> null
    } as T?

    abstract fun getObject(key: String): DataObject
}