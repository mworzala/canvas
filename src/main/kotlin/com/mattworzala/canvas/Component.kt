package com.mattworzala.canvas

interface Component<P : Props> {
    val width: Int
    val height: Int
    val handler: RenderContext<P>.() -> Unit
    val flags: Int

    operator fun invoke(context: RenderContext<P>) = handler(context)
}

class FunctionComponent<P : Props>(
    override val width: Int,
    override val height: Int,
    vararg flags: Int = intArrayOf(),
    override val handler: RenderContext<P>.() -> Unit
) : Component<P> {
    override val flags: Int = if (flags.isEmpty()) 0 else flags.reduce { acc, i -> acc or i }
}

fun <P : Props> component(width: Int, height: Int, vararg flags: Int = intArrayOf(), handler: RenderContext<P>.() -> Unit) =
    FunctionComponent(width, height, *flags, handler = handler)
