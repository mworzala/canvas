package com.mattworzala.canvas

import com.mattworzala.canvas.internal.StateDispenser

const val CLEAR_ON_RENDER: Int = 0x1
const val FORCE_STATE_UPDATE: Int = 0x2

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

    fun cleanup()

    fun onCleanup(handler: Effect)
}
