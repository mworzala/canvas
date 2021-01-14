package com.mattworzala.canvas

import com.mattworzala.canvas.internal.UseStateDelegate
import net.minestom.server.MinecraftServer
import net.minestom.server.utils.time.TimeUnit

typealias Effect = () -> Unit

fun <T> Component<*>.useState(default: T) = UseStateDelegate(this, default)

fun Component<*>.useEffectNC(handler: Effect) = useEffect { handler(); null }

@Suppress("UNUSED_VALUE")
fun Component<*>.useEffect(vararg deps: Any, handler: () -> Effect?) {
    var cleanup by useState<Effect?>(null)
    var oldDeps by useState<Array<out Any>?>(null)

    if (oldDeps != null && deps.contentEquals(oldDeps))
        return

    cleanup?.invoke()
    oldDeps = deps
    cleanup = handler()

    //todo will not call cleanup on component cleanup.
}

fun Component<*>.useUpdate(interval: Long, unit: TimeUnit, func: Effect) = useEffect {
    val task = MinecraftServer.getSchedulerManager().buildTask(func)
        .delay(interval, unit).repeat(interval, unit).schedule()
    return@useEffect task::cancel
}
