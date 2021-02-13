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
 * The class used as the base for a component hierarchy and the component DSL.
 *
 * See [SlotHolder] for slot manipulation methods.
 */
interface RenderContext<P : Props> : SlotHolder {
    val state: StateDispenser
    val rendered: Boolean
    val flags: Int

    /* Rendering */

    val props: P

    fun <P : Props> child(x: Int, y: Int, component: Component<P>, props: P, propHandler: P.() -> Unit) =
        child(getIndex(x, y), component, props, propHandler)

    fun <P : Props> child(index: Int, component: Component<P>, props: P, propHandler: P.() -> Unit)

    /* Lifecycle */

    fun render(props: P? = null)

    fun update()

    fun cleanup()

    fun onCleanup(handler: Effect)
}
