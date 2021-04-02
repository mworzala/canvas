package com.mattworzala.canvas.extra

import com.mattworzala.canvas.Fragment
import com.mattworzala.canvas.RenderContext
import com.mattworzala.canvas.useState
import net.minestom.server.data.Data

fun memo(fragment: Fragment): Fragment = MemoFragment(fragment)

/**
 * A memoized fragment, useful if a fragment is expensive to re render.
 *
 * The fragment will cache the data passed the last time it was rendered,
 * and only re render if the data changed.
 *
 * State changes still cause a rerender
 */
private class MemoFragment(val fragment: Fragment) : Fragment {
    override val width get() = fragment.width
    override val height get() = fragment.height
    override val flags get() = fragment.flags

    override val handler: RenderContext.() -> Unit = {
        var lastData by useState<Data?>(null)

        // If data is not the same, set old data & re render
        if (data != lastData) {
            lastData = data
            fragment(this)
        }
    }
}