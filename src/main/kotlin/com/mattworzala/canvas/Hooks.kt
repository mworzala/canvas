package com.mattworzala.canvas

import net.minestom.server.MinecraftServer
import java.time.Duration

typealias Effect = () -> Unit

/**
 *
 */
fun <T> RenderContext.useState(default: T) = state.get { default }

fun <T> RenderContext.useState(default: () -> T) = state.get(default)

//todo ideally should not use any state values. cleanup effects need a rework.
fun RenderContext.useCleanup(cleanup: Effect) {
    var set by state.UNSAFE_get({ false })

    if (set) return
    @Suppress("UNUSED_VALUE")
    set = true
    onCleanup(cleanup)
}

//todo this would be better if it didn't use two (3 including cleanup) state values.
fun RenderContext.useEffect(vararg deps: Any, handler: () -> Effect?) {
    var cleanup by state.UNSAFE_get<Effect?>({ null })
    var oldDeps by state.UNSAFE_get<Array<out Any>?>({ null })

    // Valid because it will get the most recent value on cleanup execution.
    useCleanup { cleanup?.invoke() }

    if (deps.contentEquals(oldDeps))
        return

    // Effect is being re executed, so call the old cleanup and re set it.
    cleanup?.invoke()
    @Suppress("UNUSED_VALUE")
    oldDeps = deps
    cleanup = handler()
}

fun RenderContext.useUpdate(duration: Duration, func: Effect) = useEffect {
    val task = MinecraftServer.getSchedulerManager().buildTask(func)
        .delay(duration).repeat(duration).schedule()
    return@useEffect task::cancel
}
