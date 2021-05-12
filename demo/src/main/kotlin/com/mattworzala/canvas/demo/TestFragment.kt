package com.mattworzala.canvas.demo

@com.mattworzala.canvas.Fragment
fun TestFragment() {
    println("HELLO WORLD ${com.mattworzala.canvas.internal.currentFragmentContext}")
}