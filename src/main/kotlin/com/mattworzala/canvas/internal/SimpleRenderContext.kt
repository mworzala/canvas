package com.mattworzala.canvas.internal

import com.mattworzala.canvas.*
import com.mattworzala.canvas.ext.InventoryHandle
import com.mattworzala.canvas.extra.all
import com.mattworzala.canvas.extra.indices
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.minestom.server.data.Data
import net.minestom.server.data.DataImpl
import net.minestom.server.inventory.Inventory
import java.util.*
import java.util.function.IntFunction

/**
 * A basic implementation of a [RenderContext]. Implementation details may vary, so this class should
 * not be used as a reference.
 */
class SimpleRenderContext(
    private val parent: SlotHolder,
    private val offset: Int,
    private val fragment: Fragment
) : RenderContext {
    private val children: Int2ObjectMap<RenderContext> = Int2ObjectOpenHashMap()
    private val cleanupEffects: MutableList<Effect> = mutableListOf()

    override val state = StateDispenser(this) { println("An error has occurred, need to cleanup here.") }

    override var rendered = false

    override val container: Inventory
        get() = parent.container

    /* Rendering */

    override val inventory: InventoryHandle = object : InventoryHandle {
        override val handle: Inventory get() = container
    }

    override fun child(index: Int, fragment: Fragment) {
        println("Adding child with id $fragment.id")

        @Suppress("UNCHECKED_CAST")
        val child: RenderContext = //todo dont just use child hashcode, can use uniqueid object
            children.computeIfAbsent(fragment.id.hashCode(), IntFunction {
                SimpleRenderContext(this, index, fragment)
            }) as RenderContext

        child.render()
    }

    /* Lifecycle */

    @Synchronized
    override fun render() {
        rendered = true

        // Reset covered slots
        indices(all, Slot::reset)

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

    override val width: Int get() = fragment.width
    override val height: Int get() = fragment.height

    override fun get(index: Int): Slot = parent.get(getIndexInParent(index))
    override fun set(index: Int, slot: Slot) = parent.set(getIndexInParent(index), slot)

    private fun getIndexInParent(index: Int) = offset + (index % width) + (parent.width * (index / width))
}