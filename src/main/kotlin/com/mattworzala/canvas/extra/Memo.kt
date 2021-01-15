package com.mattworzala.canvas.extra

import com.mattworzala.canvas.Component
import com.mattworzala.canvas.Props
import com.mattworzala.canvas.RenderContext

fun <P : Props> memo(component: Component<P>): Component<P> = MemoComponent(component)

private class MemoComponent<P : Props>(val component: Component<P>) : Component<P> {
    override val width = component.width
    override val height = component.height

    override val handler: RenderContext<P>.() -> Unit = {
        //todo

        component.handler(this)
    }
}