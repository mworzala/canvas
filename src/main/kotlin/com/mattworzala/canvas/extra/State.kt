package com.mattworzala.canvas.extra

import java.util.concurrent.CopyOnWriteArrayList

interface StateHolder {
    fun <T> get(default: T): Pair<Int, T>

    fun <T> set(index: Int, t: T)
}

class ComponentState(private val render: () -> Unit) : StateHolder {
    private val state: MutableList<Any?> = CopyOnWriteArrayList()
    private var index = 0

    fun reset() {
        if (index != state.size) throw IllegalStateException("State was removed in a non-deterministic manner. See documentation on state usage.")
        index = 0
    }

    fun <T> unsafeGet(index: Int): T {
        return state[index] as T
    }

    override fun <T> get(default: T): Pair<Int, T> {
        if (index == state.size)
            state.add(default)
        return Pair(index, state[index++] as T)
    }

    override fun <T> set(index: Int, t: T) {
        state[index] = t
        render()
    }
}
