package com.mattworzala.canvas

import com.mattworzala.canvas.internal.ComponentHolder
import com.mattworzala.canvas.internal.StateDispenser

class Component<P : Props>(
    private val parent: SlotHolder,
    private val offset: Int,
    private val comp: FunctionComponent<P>
) : ComponentHolder(), SlotHolder {
    override val width: Int get() = comp.width
    override val height: Int get() = comp.height

    private var _props: P? = null
    val props: P get() = _props!!

    val state = StateDispenser(this) { println("An error has occurred, need to cleanup here.") }

    override fun get(index: Int): Slot = parent.get(getIndexInParent(index))
    override fun set(index: Int, slot: Slot) = parent.set(getIndexInParent(index), slot)

    @Synchronized
    fun render(props: P? = null) {
        if (props != null)
            _props = props

        // Reset covered slots if flagged
        if (false) apply((0 until size).toList(), Slot::reset)

        // Call renderer
        state.pushIndex()
        comp.handler(this)
        state.popIndex()
    }

    private fun getIndexInParent(index: Int) = offset + (index % width) + (parent.width * (index / width))
}

interface BaseComponent<P : Props> {
    val width: Int
    val height: Int
    val handler: Component<P>.() -> Unit
}

class FunctionComponent<P : Props>(
    override val width: Int,
    override val height: Int,
    override val handler: Component<P>.() -> Unit
) : BaseComponent<P>

fun <P : Props> component(width: Int, height: Int, handler: Component<P>.() -> Unit) = FunctionComponent(width, height, handler)
