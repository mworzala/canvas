package com.mattworzala.canvas

import com.mattworzala.canvas.CanvasProvider.canvas
import net.minestom.server.entity.Player

// Heavily inspired by Jetpack Compose's @Composable
@Retention(AnnotationRetention.BINARY)
@Target(
    // function declarations
    // @Fragment fun Foo() { ... }
    // lambda expressions
    // val foo = @Fragment { ... }
    AnnotationTarget.FUNCTION,

    // type declarations
    // var foo: @Fragment () -> Unit = { ... }
    // parameter types
    // foo: @Fragment () -> Unit
    AnnotationTarget.TYPE,

    // fragment types inside of type signatures
    // foo: (@Fragment () -> Unit) -> Unit
    AnnotationTarget.TYPE_PARAMETER,

    // fragment property getters and setters
    // val foo: Int @Fragment get() { ... }
    // var bar: Int
    //   @Fragment get() { ... }
    AnnotationTarget.PROPERTY_GETTER
)
annotation class Fragment

class FragmentContext {

}


/**
 * A [OldFragment] is a part of a UI that can do things.
 *
 * Similar to components in modern web frameworks.
 */
interface OldFragment {
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
) : OldFragment {
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
fun fragment(width: Int = 1, height: Int = 1, vararg flags: Int = intArrayOf(), handler: RenderContext.() -> Unit) =
    FunctionFragment(width, height, *flags, handler = handler)
