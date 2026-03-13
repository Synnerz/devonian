package com.github.synnerz.devonian.utils

class FixedIdentityMap<K, V>(val maxSize: Int) : MutableMap<K, V> {
    private val delegate = object : LinkedHashMap<KeyWrapper, V>() {
        override fun removeEldestEntry(eldest: Map.Entry<KeyWrapper, V?>?): Boolean {
            return size >= maxSize
        }
    }

    override val size: Int = delegate.size
    override val keys: MutableSet<K> get() = throw UnsupportedOperationException("lazy")
    override val values: MutableCollection<V> = delegate.values
    override val entries: MutableSet<MutableMap.MutableEntry<K, V>> get() = throw UnsupportedOperationException("lazy")

    override fun get(key: K): V? {
        return delegate[KeyWrapper(key)]
    }

    override fun put(key: K, value: V): V? {
        return delegate.put(KeyWrapper(key), value)
    }

    override fun remove(key: K): V? {
        return delegate.remove(KeyWrapper(key))
    }

    override fun putAll(from: Map<out K, V>) {
        return delegate.putAll(from.entries.associate { KeyWrapper(it.key) to it.value })
    }

    override fun clear() {
        delegate.clear()
    }

    override fun isEmpty(): Boolean {
        return delegate.isEmpty()
    }

    override fun containsKey(key: K): Boolean {
        return delegate.containsKey(KeyWrapper(key))
    }

    override fun containsValue(value: V): Boolean {
        return delegate.containsValue(value)
    }

    private class KeyWrapper(val wrapped: Any?) {
        override fun equals(other: Any?): Boolean {
            if (other is KeyWrapper) return wrapped === other.wrapped
            return wrapped === other
        }

        override fun hashCode(): Int {
            return System.identityHashCode(wrapped)
        }
    }
}