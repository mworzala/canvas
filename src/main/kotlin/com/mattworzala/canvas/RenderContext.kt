package com.mattworzala.canvas

import com.mattworzala.canvas.internal.StateDispenser

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

    val props: Props

    fun child(x: Int, y: Int, fragment: Fragment, props: MutableProps = mutablePropsOf(), propHandler: Props.() -> Unit = {}) =
        child(getIndex(x, y), fragment, props, propHandler)

    fun child(index: Int, fragment: Fragment, props: MutableProps = mutablePropsOf(), propHandler: MutableProps.() -> Unit = {})

    /* Lifecycle */

    fun render(props: Props? = null)

    fun update()

    fun cleanup()

    fun onCleanup(handler: Effect)
}
