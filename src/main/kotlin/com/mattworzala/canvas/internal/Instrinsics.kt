package com.mattworzala.canvas.internal

import com.mattworzala.canvas.Fragment
import com.mattworzala.canvas.FragmentContext

val currentFragmentContext: FragmentContext
    @Fragment get() = throw NotImplementedError("Implemented as an intrinsic")

fun invokeFragment(context: FragmentContext, fragment: @Fragment () -> Unit = {}) {
    @Suppress("UNCHECKED_CAST")
    val realFn = fragment as Function2<FragmentContext, Int, Unit>
    realFn(context, 1)
}
