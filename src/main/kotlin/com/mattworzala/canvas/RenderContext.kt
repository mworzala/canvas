package com.mattworzala.canvas

import com.mattworzala.canvas.internal.StateDispenser

interface RenderContext<P : Props> : SlotHolder {
    val state: StateDispenser

    /* Rendering */

    val props: P

    fun <P : Props> child(index: Int, component: Component<P>, props: P, propHandler: P.() -> Unit)

    /* Lifecycle */

    fun render(props: P? = null)

    fun cleanup()
}
