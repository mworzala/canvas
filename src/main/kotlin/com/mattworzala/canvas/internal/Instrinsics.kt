package com.mattworzala.canvas.internal

import com.mattworzala.canvas.Fragment
import com.mattworzala.canvas.FragmentContext

val currentFragmentContext: FragmentContext
    get() { throw NotImplementedError("Implemented as an intrinsic") }

//todo should be internal
fun invokeFragment(context: FragmentContext, composable: @Fragment () -> Unit = {}) {
    @Suppress("UNCHECKED_CAST")
    val realFn = composable as Function2<FragmentContext, Int, Unit>
    realFn(context, 1)
}

//fun drawFragment(context: FragmentContext, fragment: @Fragment () -> Unit) {
fun drawFragment(context: FragmentContext, fragment: Any) {
//    (fragment as (FragmentContext) -> Unit)(context)
}
