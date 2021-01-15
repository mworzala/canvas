package com.mattworzala.canvas.extra

import com.mattworzala.canvas.BaseComponent
import com.mattworzala.canvas.Component
import com.mattworzala.canvas.Props

fun <P : Props> memo(component: BaseComponent<P>): BaseComponent<P> = MemoComponent(component)

private class MemoComponent<P : Props>(val component: BaseComponent<P>) : BaseComponent<P> {
    override val width = component.width
    override val height = component.height

    override val handler: Component<P>.() -> Unit = {
        //todo

        component.handler(this)
    }
}