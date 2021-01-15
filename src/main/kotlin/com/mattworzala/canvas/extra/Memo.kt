package com.mattworzala.canvas.extra

import com.mattworzala.canvas.Component
import com.mattworzala.canvas.Props
import com.mattworzala.canvas.RenderContext
import com.mattworzala.canvas.useState

fun <P : Props> memo(component: Component<P>): Component<P> = MemoComponent(component)

private class MemoComponent<P : Props>(val component: Component<P>) : Component<P> {
    override val width get() = component.width
    override val height get() = component.height
    override val flags get() = component.flags

    override val handler: RenderContext<P>.() -> Unit = {
        var lastProps by useState<P?>(null)

        // If props are not the same, set old props & re render
        if (props != lastProps) {
            lastProps = props
            component(this)
        }
    }
}