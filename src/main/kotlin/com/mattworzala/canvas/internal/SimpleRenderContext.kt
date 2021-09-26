package com.mattworzala.canvas.internal

import com.mattworzala.canvas.*
import com.mattworzala.canvas.ext.InventoryHandle
import com.mattworzala.canvas.extra.all
import com.mattworzala.canvas.extra.indices
import net.minestom.server.entity.Player
import net.minestom.server.inventory.Inventory
import net.minestom.server.item.ItemStack
import java.util.*

/**
 * A basic implementation of a [RenderContext]. Implementation details may vary, so this class should
 * not be used as a reference.
 */
class SimpleRenderContext(
    private val parent: SlotHolder,
    private val offset: Int,
    private var fragment: Fragment
) : RenderContext {

    private val children: MutableMap<Int, RenderContext> = mutableMapOf()
    private val cleanupEffects: MutableList<Effect> = mutableListOf()

    override val state = StateDispenser(this) { println("An error has occurred, need to cleanup here.") }

    override var rendered = false

    override val container: Inventory
        get() = parent.container
    override val owner: Player
        get() = parent.owner

    /* Rendering */

    override val inventory: InventoryHandle = object : InventoryHandle {
        override val handle: Inventory get() = container
    }

    override fun child(index: Int, fragment: Fragment) {
        @Suppress("UNCHECKED_CAST")
        val child: SimpleRenderContext =
            children.computeIfAbsent(Objects.hash(index, fragment.id)) {
                SimpleRenderContext(this, index, fragment)
            } as SimpleRenderContext
        child.fragment = fragment // Must use the new fragment to account for new props

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

    override fun get(index: Int): Slot = parent[getIndexInParent(index)]
    override fun set(index: Int, slot: Slot) = parent.set(getIndexInParent(index), slot)

    private fun getIndexInParent(index: Int) = offset + (index % width) + (parent.width * (index / width))

    /* Auto formatting */

    var currentIndex = 0

}