package com.mattworzala.canvas.extra

import com.mattworzala.canvas.*

fun <P : OldProps> memo(fragment: Fragment): Fragment = MemoFragment(fragment)

/**
 * A memoized fragment, useful if a fragment is expensive to re render.
 *
 * The fragment will cache the props passed the last time it was rendered,
 * and only re render if the props changed.
 *
 * State changes still cause a rerender
 */
private class MemoFragment(val fragment: Fragment) : Fragment {
    override val width get() = fragment.width
    override val height get() = fragment.height
    override val flags get() = fragment.flags

    override val handler: RenderContext.() -> Unit = {
        var lastProps by useState<Props?>(null)

        // If props are not the same, set old props & re render
        if (props != lastProps) {
            lastProps = props
            fragment(this)
        }
    }
}