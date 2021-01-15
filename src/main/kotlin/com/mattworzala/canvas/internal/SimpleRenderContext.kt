package com.mattworzala.canvas.internal

import com.mattworzala.canvas.*
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import java.util.*

class SimpleRenderContext<P : Props>(
    private val parent: SlotHolder,
    private val offset: Int,
    private val component: Component<P>
) : RenderContext<P> {
    private val children: Int2ObjectMap<RenderContext<*>> = Int2ObjectOpenHashMap()

    override val state = StateDispenser(this) { println("An error has occurred, need to cleanup here.") }

    /* Rendering */

    private var _props: P? = null
    override val props: P get() = _props!!

    override fun <P : Props> child(index: Int, component: Component<P>, props: P, propHandler: P.() -> Unit) {
        val childId = Objects.hash(index, component)

        @Suppress("UNCHECKED_CAST")
        val child: RenderContext<P> =
            children.computeIfAbsent(childId) { SimpleRenderContext(this, index, component) } as RenderContext<P>
        props.propHandler()
        child.render(props)
    }

    /* Lifecycle */

    @Synchronized
    override fun render(props: P?) {
        if (props != null)
            _props = props

        // Reset covered slots if flagged
        if (false) apply((0 until size).toList(), Slot::reset)

        // Call renderer
        state.pushIndex()
        component(this)
        state.popIndex()
    }

    @Synchronized
    override fun cleanup() {
        children.values.forEach(RenderContext<*>::cleanup)

        //todo cleanup tasks
    }

    override val width: Int get() = component.width
    override val height: Int get() = component.height

    override fun get(index: Int): Slot = parent.get(getIndexInParent(index))
    override fun set(index: Int, slot: Slot) = parent.set(getIndexInParent(index), slot)

    private fun getIndexInParent(index: Int) = offset + (index % width) + (parent.width * (index / width))
}