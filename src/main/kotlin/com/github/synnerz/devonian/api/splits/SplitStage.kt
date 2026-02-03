package com.github.synnerz.devonian.api.splits

import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.StringUtils

open class SplitStage {
    val children: Array<SplitStage>
    var parent: SplitStage? = null
        protected set
    protected val filterStr: String?
    protected val filterReg: Regex?
    var startTime = TimeUnit.EMPTY
    var stopTime = TimeUnit.EMPTY
    val isActiveState = BasicState(false)
    val hasStartedState = BasicState(false)
    val hasFinishedState = BasicState(false)
    var name = ""
    var shortenTime = true

    fun withName(name: String) = apply {
        this.name = name
    }
    fun withLongTime() = apply {
        shortenTime = false
    }

    constructor(children: Array<SplitStage> = arrayOf()) {
        this.children = children
        children.forEach { it.parent = this }
        filterStr = null
        filterReg = null
    }
    constructor(filter: String, children: Array<SplitStage> = arrayOf()) {
        this.children = children
        children.forEach { it.parent = this }
        filterStr = filter
        filterReg = null
    }
    constructor(filter: Regex, children: Array<SplitStage> = arrayOf()) {
        this.children = children
        children.forEach { it.parent = this }
        filterStr = null
        filterReg = filter
    }

    /**
     * <code>hasStarted() && !hasFinished()</code>
     */
    open fun isActive() = stopTime < startTime
    open fun hasStarted() = !startTime.isEmpty()
    open fun hasFinished() = !stopTime.isEmpty()

    open fun reset() {
        startTime = TimeUnit.EMPTY
        stopTime = TimeUnit.EMPTY
        isActiveState.value = false
        hasStartedState.value = false
        hasFinishedState.value = false

        children.forEach { it.reset() }
    }

    open fun onChat(msg: String) {
        var b = false
        if (filterStr != null) {
            if (msg == filterStr) start()
        } else if (filterReg != null) {
            if (filterReg.matches(msg)) start()
        } else b = true

        if (isActive() || b) children.forEach {
            it.onChat(msg)
        }
    }

    protected open fun _start() {
        isActiveState.value = true
        hasStartedState.value = true
        startTime = TimeUnit.now()
    }

    protected open fun _stop() {
        isActiveState.value = false
        hasFinishedState.value = true
        stopTime = TimeUnit.now()
    }

    open fun start() {
        if (hasStarted()) return
        _start()
        parent?.onChildStart(this)

        children.forEach { it.onParentStart() }
    }

    open fun stop() {
        if (!isActive()) return
        _stop()
        parent?.onChildStop(this)

        children.forEach { it.onParentStop() }
    }

    open fun onParentStart() {
        start()
    }

    open fun onParentStop() {
        stop()
    }

    open fun onChildStart(child: SplitStage) {
        start()
    }
    open fun onChildStop(child: SplitStage) {}

    open fun getTime(force: TimeUnit? = null): TimeUnit {
        val stop = if (stopTime.isEmpty()) TimeUnit.now() else stopTime
        val start = force ?: startTime
        return stop.diff(start)
    }

    open fun formatTime(ms: Long): String {
        return if (shortenTime) StringUtils.formatTime(ms, 2)
        else "%.2fs".format(ms / 1000.0)
    }

    fun format(format: TimeUnit.Format, force: TimeUnit? = null): String {
        val time = getTime(force)
        return when (format) {
            TimeUnit.Format.RealTime -> "${name}&r&f: &a${formatTime(time.unix)}"
            TimeUnit.Format.ServerTick -> "${name}&r&f: &b${formatTime(time.tick * 50L)}"
            TimeUnit.Format.Both -> "${name}&r&f: &a${formatTime(time.unix)} &7(&b${formatTime(time.tick * 50L)}&7)"
        }
    }

    open fun getThisSplit(format: TimeUnit.Format, force: TimeUnit? = null): MutableList<String> {
        if (name == "") return mutableListOf()
        return mutableListOf(format(format, force))
    }

    open fun getSplits(format: TimeUnit.Format, force: TimeUnit? = null): List<String> {
        if (!hasStarted() && force == null) return emptyList()
        return children.flatMapTo(getThisSplit(format, force)) { it.getSplits(format, force) }
    }
}