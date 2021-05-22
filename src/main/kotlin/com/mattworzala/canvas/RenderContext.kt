package com.mattworzala.canvas

import com.mattworzala.canvas.ext.InventoryHandle
import com.mattworzala.canvas.internal.StateDispenser
import net.minestom.server.data.Data
import net.minestom.server.data.DataImpl

/**
 * If this flag is set, the UI will be reset before a render.
 * todo not implemented, perhaps it should be default?
 */
const val CLEAR_ON_RENDER: Int = 0x1

/**
 * By default, when a state value is changed a re-render only occurs
 * if the new value is different from the old value. This is to save
 * time, however when a UI relies on re renders to update values this
 * becomes problematic.
 *
 * When this flag is set, there will always be a state update even if
 * the new value is the same.
 */
const val FORCE_STATE_UPDATE: Int = 0x2

/**
 * The class used as the base for a fragment hierarchy and the fragment DSL.
 *
 * See [SlotHolder] for slot manipulation methods.
 */
interface RenderContext : SlotHolder {
    val state: StateDispenser
    val rendered: Boolean
    val flags: Int

    /* Rendering */

    val inventory: InventoryHandle
    val data: Data

    fun child(x: Int, y: Int, fragment: Fragment, data: Data = DataImpl(), dataHandler: Data.() -> Unit = {}) =
        child(getIndex(x, y), fragment, data, dataHandler)

    fun child(index: Int, fragment: Fragment, data: Data = DataImpl(), dataHandler: Data.() -> Unit = {})

    /* State safety */

    //todo docs, should have an unsafe mutable property on the delegate which errors if you use it outside of a mutable block.
    fun <T> mutate(mutator: () -> T?): T? {
        val result = mutator()
        render()
        return result
    }

    /* Lifecycle */

    fun render(data: Data? = null)

    fun update()

    fun cleanup()

    fun onCleanup(handler: Effect)

    fun put(fragment: Fragment, index: Int, dataHandler: Data.() -> Unit = {}) = child(index, fragment, dataHandler = dataHandler)
}
