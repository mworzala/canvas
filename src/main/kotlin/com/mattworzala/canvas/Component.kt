package com.mattworzala.canvas

import com.mattworzala.canvas.CanvasProvider.canvas
import net.minestom.server.entity.Player

/**
 * A component is a part of a UI that can do things.
 *
 * Similar to components in modern web frameworks.
 */
interface Component {

    /** The width of the component. */
    val width: Int
    /** The height of the component*/
    val height: Int
    /** Kotlin DSL to define behavior of the component */
    val handler: RenderContext.() -> Unit
    /** The flags of a component. Defined in [RenderContext] */
    val flags: Int
    /** Invokes the [handler] DSL */
    operator fun invoke(context: RenderContext) = handler(context)

    fun render(player: Player) {
        val canvas = canvas(player)
        canvas.render(this)
    }
}

/**
 * Implementation of a Component. Can run actions against a UI.
 */
class FunctionComponent(
    override val width: Int,
    override val height: Int,
    /** The flags as a vararg. Used for simplifying constructors. */
    vararg flags: Int = intArrayOf(),
    override val handler: RenderContext.() -> Unit
) : Component {
    override val flags: Int = if (flags.isEmpty()) 0 else flags.reduce { acc, i -> acc or i }
}

/**
 * Kotlin DSL for constructing [FunctionComponent]s
 *
 * @param width The width of the component
 * @param height The height of the component
 * @param flags The flags of the component as a vararg for easy constructing
 * @param handler The kotlin DSL for [FunctionComponent]. Used for defining UI behavior of that component.
 *
 * @return The constructed [FunctionComponent]
 */
fun component(width: Int, height: Int, vararg flags: Int = intArrayOf(), handler: RenderContext.() -> Unit) =
    FunctionComponent(width, height, *flags, handler = handler)
