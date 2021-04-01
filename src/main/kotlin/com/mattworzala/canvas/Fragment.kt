package com.mattworzala.canvas

import com.mattworzala.canvas.CanvasProvider.canvas
import net.minestom.server.data.Data
import net.minestom.server.entity.Player

/**
 * A [Fragment] is a part of a UI that can do things.
 *
 * Similar to components in modern web frameworks.
 */
interface Fragment {
    /** The width of the fragment. */
    val width: Int
    
    /** The height of the fragment*/
    val height: Int
    
    /** Kotlin DSL to define behavior of the fragment */
    val handler: RenderContext.() -> Unit
    
    /** The flags of a fragment. Defined in [RenderContext] */
    val flags: Int
    
    /** Invokes the [handler] DSL */
    operator fun invoke(context: RenderContext) = handler(context)

    operator fun invoke(context: RenderContext, index: Int, dataHandler: Data.() -> Unit = {}) = context.child(index, this, dataHandler = dataHandler)

    fun render(player: Player) {
        val canvas = canvas(player)
        canvas.render(this)
    }
}

/**
 * Implementation of a Fragment. Can run actions against a UI.
 */
class FunctionFragment(
    override val width: Int,
    override val height: Int,
    /** The flags as a vararg. Used for simplifying constructors. */
    vararg flags: Int = intArrayOf(),
    override val handler: RenderContext.() -> Unit
) : Fragment {
    override val flags: Int = if (flags.isEmpty()) 0 else flags.reduce { acc, i -> acc or i }
}

/**
 * Kotlin DSL for constructing [FunctionFragment]s
 *
 * @param width The width of the fragment
 * @param height The height of the fragment
 * @param flags The flags of the fragment as a vararg for easy constructing
 * @param handler The kotlin DSL for [FunctionFragment]. Used for defining UI behavior of that fragment.
 *
 * @return The constructed [FunctionFragment]
 */
fun fragment(width: Int, height: Int, vararg flags: Int = intArrayOf(), handler: RenderContext.() -> Unit) =
    FunctionFragment(width, height, *flags, handler = handler)
