package com.mattworzala.canvas.internal

import com.mattworzala.canvas.*
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.minestom.server.data.Data
import net.minestom.server.data.DataImpl
import java.util.*
import java.util.function.IntFunction

/**
 * A basic implementation of a [RenderContext]. Implementation details may vary, so this class should
 * not be used as a reference.
 */
class SimpleRenderContext(
    private val parent: SlotHolder,
    private val offset: Int,
    private val fragment: OldFragment
) : RenderContext {
    private val children: Int2ObjectMap<RenderContext> = Int2ObjectOpenHashMap()
    private val cleanupEffects: MutableList<Effect> = mutableListOf()

    override val state = StateDispenser(this) { println("An error has occurred, need to cleanup here.") }

    override var rendered = false

    /* Rendering */

    private var _data: Data? = null
    override val data: Data
        get() = _data ?: DataImpl()

    override fun child(index: Int, fragment: OldFragment, data: Data, dataHandler: Data.() -> Unit) {
        val childId = Objects.hash(index, fragment)

        @Suppress("UNCHECKED_CAST")
        val child: RenderContext =
            children.computeIfAbsent(childId, IntFunction {
                SimpleRenderContext(this, index, fragment)
            }) as RenderContext

        this.data.dataHandler()
        child.render(this.data)
    }

    /* Lifecycle */

    @Synchronized
    override fun render(data: Data?) {
        rendered = true
        if (data != null) {
            _data = data
        }

        // Reset covered slots if flagged
//        if (flags has CLEAR_ON_RENDER) apply((0 until size).toList(), Slot::reset)

        // Call renderer
        state.pushIndex()
        fragment(this)
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

    override val flags: Int get() = fragment.flags
    override val width: Int get() = fragment.width
    override val height: Int get() = fragment.height

    override fun get(index: Int): Slot = parent.get(getIndexInParent(index))
    override fun set(index: Int, slot: Slot) = parent.set(getIndexInParent(index), slot)

    private fun getIndexInParent(index: Int) = offset + (index % width) + (parent.width * (index / width))
}