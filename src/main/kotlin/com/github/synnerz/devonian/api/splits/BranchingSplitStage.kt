package com.github.synnerz.devonian.api.splits

import com.github.synnerz.devonian.utils.BasicState

class BranchingSplitStage : SplitStage {
    constructor(children: Array<SplitStage>) : super(children)
    constructor(filter: String, children: Array<SplitStage> = arrayOf()) : super(filter, children)
    constructor(filter: Regex, children: Array<SplitStage> = arrayOf()) : super(filter, children)

    var chosenChild: SplitStage? = null
    val chosenChildState: BasicState<SplitStage?> = BasicState(null)

    override fun start() {
        if (hasStarted()) return
        _start()
        parent?.onChildStart(this)
    }

    override fun reset() {
        super.reset()
        chosenChild = null
        chosenChildState.value = null
    }

    override fun onChat(msg: String) {
        if (chosenChild != null) chosenChild?.onChat(msg)
        else children.forEach { it.onChat(msg) }
    }

    override fun onChildStart(child: SplitStage) {
        super.onChildStart(child)
        children.forEach {
            if (it !== child) it.stop()
        }
        chosenChild = child
        chosenChildState.value = child
    }

    override fun getSplits(format: TimeUnit.Format, force: TimeUnit?): List<String> {
        if (!hasStarted() && force == null) return emptyList()
        val list = getThisSplit(format, force)
        list.addAll(chosenChild?.getSplits(format, force) ?: emptyList())
        return list
    }
}