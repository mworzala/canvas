package com.mattworzala.canvas.internal

import com.mattworzala.canvas.Fragment
import com.mattworzala.canvas.FragmentContext

val currentFragmentContext: FragmentContext
    get() { throw NotImplementedError("Implemented as an intrinsic") }

//fun drawFragment(context: FragmentContext, fragment: @Fragment () -> Unit) {
fun drawFragment(context: FragmentContext, fragment: Any) {
    (fragment as (FragmentContext) -> Unit)(context)
}
