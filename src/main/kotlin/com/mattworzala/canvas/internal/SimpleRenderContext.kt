package com.mattworzala.canvas.internal

import com.mattworzala.canvas.*
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import java.util.*

/**
 * A basic implementation of a [RenderContext]. Implementation details may vary, so this class should
 * not be used as a reference.
 */
class SimpleRenderContext(
    private val parent: SlotHolder,
    private val offset: Int,
    private val component: Component
) : RenderContext {
    private val children: Int2ObjectMap<RenderContext> = Int2ObjectOpenHashMap()
    private val cleanupEffects: MutableList<Effect> = mutableListOf()

    override val state = StateDispenser(this) { println("An error has occurred, need to cleanup here.") }

    override var rendered = false

    /* Rendering */

    private var _props: MutableProps? = null
    override val props: Props get() = _props!!

    override fun child(index: Int, component: Component, props: MutableProps, propHandler: MutableProps.() -> Unit) {
        val childId = Objects.hash(index, component)

        @Suppress("UNCHECKED_CAST")
        val child: RenderContext =
            children.computeIfAbsent(childId) { SimpleRenderContext(this, index, component) } as RenderContext
        props.propHandler()
        child.render(props)
    }

    /* Lifecycle */

    @Synchronized
    override fun render(props: Props?) {
        rendered = true
        if (props != null) {
            if (props !is MutableProps)
                throw IllegalArgumentException("Only mutable props are supported at this time!")
            _props = props
        }

        // Reset covered slots if flagged
//        if (flags has CLEAR_ON_RENDER) apply((0 until size).toList(), Slot::reset)

        // Call renderer
        state.pushIndex()
        component(this)
        state.popIndex()
    }

    override fun update() {
        //todo fixed update
    }

    @Synchronized
    override fun cleanup() {
        children.values.forEach(RenderContext::cleanup)

        cleanupEffects.forEach(Effect::invoke)
        rendered = false
    }

    override fun onCleanup(handler: Effect) {
        cleanupEffects.add(handler)
    }

    override val flags: Int get() = component.flags
    override val width: Int get() = component.width
    override val height: Int get() = component.height

    override fun get(index: Int): Slot = parent.get(getIndexInParent(index))
    override fun set(index: Int, slot: Slot) = parent.set(getIndexInParent(index), slot)

    private fun getIndexInParent(index: Int) = offset + (index % width) + (parent.width * (index / width))
}