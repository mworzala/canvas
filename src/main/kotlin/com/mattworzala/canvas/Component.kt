package com.mattworzala.canvas

import com.mattworzala.canvas.extra.ComponentState
import com.mattworzala.canvas.internal.ComponentHolder

open class Component<P : Props>(
    private val parent: SlotHolder,
    private val offset: Int,
    private val comp: FunctionComponent<P>
) : ComponentHolder(), SlotHolder {
    override val width: Int get() = comp.width
    override val height: Int get() = comp.height

    @Volatile
    private var shouldRender = false
    private var _props: P? = null
    val props: P get() = _props!!

    val state = ComponentState { shouldRender = true }

    override fun get(index: Int): Slot = parent.get(getIndexInParent(index))
    override fun set(index: Int, slot: Slot) = parent.set(getIndexInParent(index), slot)

    open fun render(props: P) {
        _props = props
        shouldRender = true
    }

    override fun update() {
        if (shouldRender)
            unsafeRender()

        super.update()
    }

    private fun unsafeRender() {
        shouldRender = false

        // Reset covered slots if flagged
        if (false) apply((0 until size).toList(), Slot::reset)

        // Call renderer
        comp.handler(this)

        // Reset state counter
        state.reset()
    }

    private fun getIndexInParent(index: Int) = offset + (index % width) + (parent.width * (index / width))
}

class FunctionComponent<P : Props>(
    val width: Int,
    val height: Int,
    val handler: Component<P>.() -> Unit
)
