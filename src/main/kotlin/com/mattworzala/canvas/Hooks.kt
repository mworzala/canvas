package com.mattworzala.canvas

import net.minestom.server.MinecraftServer
import net.minestom.server.utils.time.TimeUnit

typealias Effect = () -> Unit

fun <T> RenderContext<*>.useState(default: T) = state.get(default)

// todo this could use a rework.
@Suppress("UNUSED_VALUE")
fun RenderContext<*>.useEffect(vararg deps: Any, handler: () -> Effect?) {
    var cleanup by state.UNSAFE_get<Effect?>(null)
    var oldDeps by state.UNSAFE_get<Array<out Any>?>(null)

    // Will only be the case on first render, since `deps` cannot be null.
    if (oldDeps == null) {
        //todo cleanup tasks
//        cleanupTasks.add { cleanup?.invoke() }
    }

    if (deps.contentEquals(oldDeps))
        return

    cleanup?.invoke()
    oldDeps = deps
    cleanup = handler()
}

fun RenderContext<*>.useUpdate(interval: Long, unit: TimeUnit, func: Effect) = useEffect {
    val task = MinecraftServer.getSchedulerManager().buildTask(func)
        .delay(interval, unit).repeat(interval, unit).schedule()
    return@useEffect task::cancel
}
