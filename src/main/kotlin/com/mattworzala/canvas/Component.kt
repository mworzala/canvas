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
    override val handler: RenderContext<P>.() -> Unit,
    vararg flags: Int
) : Component<P> {
    override val flags: Int = flags.reduce { acc, i -> acc or i }
}

fun <P : Props> component(width: Int, height: Int, handler: RenderContext<P>.() -> Unit) = FunctionComponent(width, height, handler)
