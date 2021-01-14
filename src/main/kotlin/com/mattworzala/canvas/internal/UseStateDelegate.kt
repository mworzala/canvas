package com.mattworzala.canvas.internal

import com.mattworzala.canvas.Component
import kotlin.reflect.KProperty

class UseStateDelegate<T> internal constructor(private val component: Component<*>, default: T) {
    private val index: Int

    init {
        val (i, _) = component.state.get(default)
        index = i
    }

    operator fun getValue(nothing: Any?, property: KProperty<*>): T {
        return component.state.unsafeGet(index)
    }

    operator fun setValue(nothing: Any?, property: KProperty<*>, value: T) {
        component.state.set(index, value)
    }
}