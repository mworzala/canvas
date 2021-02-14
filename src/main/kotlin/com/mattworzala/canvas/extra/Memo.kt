package com.mattworzala.canvas.extra

import com.mattworzala.canvas.*

fun <P : OldProps> memo(component: Component): Component = MemoComponent(component)

/**
 * A memoized component, useful if a component is expensive to re render.
 *
 * The component will cache the props passed the last time it was rendered,
 * and only re render if the props changed.
 *
 * State changes still cause a rerender
 */
private class MemoComponent(val component: Component) : Component {
    override val width get() = component.width
    override val height get() = component.height
    override val flags get() = component.flags

    override val handler: RenderContext.() -> Unit = {
        var lastProps by useState<Props?>(null)

        // If props are not the same, set old props & re render
        if (props != lastProps) {
            lastProps = props
            component(this)
        }
    }
}