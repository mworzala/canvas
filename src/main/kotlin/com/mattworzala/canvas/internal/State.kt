package com.mattworzala.canvas.internal

import com.mattworzala.canvas.RenderContext
import com.mattworzala.canvas.useState
import it.unimi.dsi.fastutil.ints.IntArrayList
import org.jetbrains.annotations.Contract
import kotlin.reflect.KProperty

private const val MAX_RENDER_DEPTH = 25

private const val ERR_RENDER_ORDER = "Rendering order incorrectly. This should not have occurred."
private const val ERR_STATE_REMOVAL = "State was removed in a non-deterministic manner. See documentation on state usage."
private const val ERR_RENDER_DEPTH = "Maximum render depth reached. This likely indicates a render loop in one of your fragments."
private const val ERR_INVALID_UPDATE = "A state update was made to a fragment which is not rendered. This indicates that an effect was not cleaned up properly."

/**
 * An internal state management tool. This should not be used externally, see the provided
 * [useState] hook for state management.
 */
class StateDispenser(
    private val context: RenderContext,
    private val handleError: () -> Unit
) {
    //todo ensure this doesn't need to be thread safe
    private val state: MutableList<StateDelegate<*>> = mutableListOf()

    private val indices: IntArrayList = IntArrayList()
    private var index: Int = 0

    fun <T> get(default: T): StateDelegate<T> = UNSAFE_get(default)

    @Suppress("FunctionName")
    fun <T> UNSAFE_get(default: T, unsafe: Boolean = false): StateDelegate<T> {
        if (index == state.size)
            state.add(StateDelegate(context, default, unsafe))
        return state[index++] as StateDelegate<T>
    }

    fun pushIndex() {
        if (indices.size > MAX_RENDER_DEPTH)
            error(ERR_RENDER_DEPTH)
        indices.push(index)
        index = 0
    }

    fun popIndex() {
        if (indices.isEmpty)
            error(ERR_RENDER_ORDER)

        if (index != state.size)
            error(ERR_STATE_REMOVAL)

        index = indices.popInt()
    }

    @Contract("_ -> fail")
    private fun error(message: String) {
        handleError()
        throw IllegalStateException(message)
    }
}

class StateDelegate<T> internal constructor(private val context: RenderContext, default: T, private val unsafe: Boolean) {
    private var value: T = default

    operator fun getValue(nothing: Any?, property: KProperty<*>): T = value

    operator fun setValue(nothing: Any?, property: KProperty<*>, value: T) {
        if (!context.rendered)
            throw IllegalStateException(ERR_INVALID_UPDATE)

        if (this.value == value) // && !context.flags.has(FORCE_STATE_UPDATE)
            return

        this.value = value
        if (!unsafe) context.render()
    }
}