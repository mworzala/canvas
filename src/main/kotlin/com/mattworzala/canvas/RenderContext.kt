package com.mattworzala.canvas

import com.mattworzala.canvas.ext.InventoryHandle
import com.mattworzala.canvas.internal.StateDispenser
import net.minestom.server.data.Data
import net.minestom.server.data.DataImpl

/**
 * The class used as the base for a fragment hierarchy and the fragment DSL.
 *
 * See [SlotHolder] for slot manipulation methods.
 */
interface RenderContext : SlotHolder {
    val state: StateDispenser
    val rendered: Boolean

    /* Rendering */

    val inventory: InventoryHandle

    fun child(x: Int, y: Int, fragment: Fragment) =
        child(getIndex(x, y), fragment)

    fun child(index: Int, fragment: Fragment)

    /* State safety */

    //todo docs, should have an unsafe mutable property
    // on the delegate which errors if you use it outside of a mutable block.
    operator fun <T> (() -> T?).not() = invoke().also { render() }

    /* Lifecycle */

    fun render()

    fun update()

    fun cleanup()

    fun onCleanup(handler: Effect)

    fun put(fragment: Fragment, index: Int) = child(index, fragment)
}
