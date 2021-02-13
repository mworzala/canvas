package com.mattworzala.canvas.extra

import com.mattworzala.canvas.Component
import com.mattworzala.canvas.Props
import com.mattworzala.canvas.RenderContext
import com.mattworzala.canvas.useState

fun <P : Props> memo(component: Component<P>): Component<P> = MemoComponent(component)

/**
 * A memoized component, useful if a component is expensive to re render.
 *
 * The component will cache the props passed the last time it was rendered,
 * and only re render if the props changed.
 *
 * State changes still cause a rerender
 */
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