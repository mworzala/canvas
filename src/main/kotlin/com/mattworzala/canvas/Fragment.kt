package com.mattworzala.canvas

import com.mattworzala.canvas.CanvasProvider.canvas
import com.mattworzala.canvas.internal.HashCodeUniqueId
import com.mattworzala.canvas.internal.StackFrameUniqueId
import com.mattworzala.canvas.internal.UniqueId
import net.minestom.server.entity.Player

/**
 * A [Fragment] is a part of a UI that can do things.
 *
 * Similar to components in modern web frameworks.
 */
interface Fragment {
    val id: UniqueId

    /** The width of the fragment. */
    val width: Int
    
    /** The height of the fragment*/
    val height: Int
    
    /** Kotlin DSL to define behavior of the fragment */
    val handler: RenderContext.() -> Unit
    
    /** Invokes the [handler] DSL */
    operator fun invoke(context: RenderContext) = handler(context)

    fun render(player: Player) {
        val canvas = canvas(player)
        canvas.render(this)
    }
}

class SmartFragment internal constructor(
    override val width: Int,
    override val height: Int,
    override val handler: RenderContext.() -> Unit
) : Fragment {
    override val id = StackFrameUniqueId()
}

fun fragment(width: Int = 1, height: Int = 1, handler: RenderContext.() -> Unit) =
    SmartFragment(width, height, handler)
