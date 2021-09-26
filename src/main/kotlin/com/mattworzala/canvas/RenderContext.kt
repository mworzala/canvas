package com.mattworzala.canvas

import com.mattworzala.canvas.ext.InventoryHandle
import com.mattworzala.canvas.internal.StateDispenser

//todo need to also rework memoization in this new update. it is relevant again since there can be expensive computation.

//todo could provide a utility to create an effect on a Minestom event. An example could be rendering delta player position using move event

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

    /**
     * Creates a "mutation block" which will directly re render the fragment
     * when its scope ends (the end of the block). Will also return the result
     * of the block if there is a return value.
     *
     * This is used when state values are mutable. I cannot track updates to
     * to them via their own mutation, so you can tell canvas that you are
     * making updates and it will re render.
     *
     * ```
     * class MyMutableClass(var name: String)
     * val myMutableState by useState(MyMutableClass("Canvas"))
     *
     * onClick {
     *     // Note the mutation block, since we are doing an indirect update.
     *     !{ myMutableState.name = event.player.username }
     * }
     * ```
     *
     * @param T The return type of the block, in case a variable needs to be elevated
     */
    operator fun <T> (() -> T).not() = invoke().also { render() }

    /* Lifecycle */

    fun render()

    fun update()

    fun cleanup()

    fun onCleanup(handler: Effect)

    fun put(fragment: Fragment, index: Int) = child(index, fragment)

    fun put(fragment: Fragment, x: Int, y: Int) = child(getIndex(x, y), fragment)
}
