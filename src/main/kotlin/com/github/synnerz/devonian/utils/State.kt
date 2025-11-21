package com.github.synnerz.devonian.utils

import kotlinx.atomicfu.atomic
import java.util.concurrent.CopyOnWriteArrayList

interface State<T> {
    var value: T
    fun listen(cb: (v: T) -> Unit)
    fun <R> map(transform: (v: T) -> R): State<R>
    fun <O, R> zip(other: State<O>, transform: (a: T, b: O) -> R): State<R>
    fun debug(name: String) = apply {
        listen {
            println("$name changed to $it")
        }
    }
}

open class BasicState<T>(initial: T) : State<T> {
    private val v = atomic(initial)
    private val listeners = CopyOnWriteArrayList<(v: T) -> Unit>()

    override var value: T
        get() = v.value
        set(value) {
            if (v.getAndSet(value) != value) listeners.forEach { it(value) }
        }

    override fun listen(cb: (v: T) -> Unit) {
        listeners.add(cb)
    }

    override fun <R> map(transform: (v: T) -> R) = UnaryDerivedState(this, transform)

    override fun <O, R> zip(other: State<O>, transform: (a: T, b: O) -> R) =
        BinaryDerivedState(this, other, transform)
}

class UnaryDerivedState<T, R>(
    base: State<T>,
    private val transform: (v: T) -> R
) : BasicState<R>(transform(base.value)) {
    override var value: R
        get() = super.value
        set(value) {
            throw UnsupportedOperationException()
        }

    init {
        base.listen {
            super.value = transform(it)
        }
    }
}

class BinaryDerivedState<T1, T2, R>(
    private val base1: State<T1>,
    private val base2: State<T2>,
    private val transform: (a: T1, b: T2) -> R
) : BasicState<R>(transform(base1.value, base2.value)) {
    override var value: R
        get() = super.value
        set(value) {
            throw UnsupportedOperationException()
        }

    init {
        base1.listen {
            super.value = transform(it, base2.value)
        }
        base2.listen {
            super.value = transform(base1.value, it)
        }
    }
}
