package com.mattworzala.canvas

interface Component<P : Props> {
    val width: Int
    val height: Int
    val handler: RenderContext<P>.() -> Unit

    operator fun invoke(context: RenderContext<P>) = handler(context)
}

class FunctionComponent<P : Props>(
    override val width: Int,
    override val height: Int,
    override val handler: RenderContext<P>.() -> Unit
) : Component<P>

fun <P : Props> component(width: Int, height: Int, handler: RenderContext<P>.() -> Unit) = FunctionComponent(width, height, handler)
