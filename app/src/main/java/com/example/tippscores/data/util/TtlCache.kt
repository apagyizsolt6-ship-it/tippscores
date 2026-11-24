package com.example.tippscores.data.util

/**
 * Egyszerű, memóriabeli gyorsítótár lejárati idővel. A StatPal hívások
 * számát csökkenti: ugyanazt a választ nem kérjük le újra, amíg friss.
 */
class TtlCache<T : Any>(
    private val maxEntries: Int = 150
) {

    private class Entry<T>(
        val savedAt: Long,
        val value: T
    )

    private val map = LinkedHashMap<String, Entry<T>>()

    @Synchronized
    fun get(key: String, ttlMs: Long): T? {
        val entry = map[key] ?: return null
        return if (System.currentTimeMillis() - entry.savedAt < ttlMs) {
            entry.value
        } else {
            map.remove(key)
            null
        }
    }

    @Synchronized
    fun put(key: String, value: T) {
        map.remove(key)
        map[key] = Entry(System.currentTimeMillis(), value)
        while (map.size > maxEntries) {
            val oldest = map.keys.firstOrNull() ?: break
            map.remove(oldest)
        }
    }

    @Synchronized
    fun clear() {
        map.clear()
    }
}
