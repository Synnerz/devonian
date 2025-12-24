package com.github.synnerz.devonian.api.splits

import com.github.synnerz.devonian.api.events.EventBus

data class TimeUnit(val unix: Long, val tick: Int) : Comparable<TimeUnit> {
    fun diff(time: TimeUnit) = TimeUnit(unix - time.unix, tick - time.tick)
    fun isEmpty() = this === EMPTY

    override fun compareTo(other: TimeUnit): Int {
        return unix.compareTo(other.unix)
    }

    enum class Format {
        RealTime, ServerTick, Both;
    }

    companion object {
        val EMPTY = TimeUnit(0L, 0)

        fun now() = TimeUnit(System.currentTimeMillis(), EventBus.serverTicks())
    }
}