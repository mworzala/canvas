package com.mattworzala.canvas.demo

import com.mattworzala.canvas.Fragment
import com.mattworzala.canvas.FragmentContext
import com.mattworzala.canvas.internal.currentFragmentContext
import com.mattworzala.canvas.internal.invokeFragment

@Fragment
fun TestFragment() {
    println("HELLO WORLD $currentFragmentContext")
}

fun main() {
    invokeFragment(FragmentContext()) {
        TestFragment()
    }
}