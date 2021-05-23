package com.mattworzala.canvas.compiler.debuglog

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import compile
import org.intellij.lang.annotations.Language
import org.junit.Test
import kotlin.test.assertEquals

@Language("kotlin")
const val testCode = """
import com.mattworzala.canvas.Fragment
import com.mattworzala.canvas.FragmentContext
import com.mattworzala.canvas.internal.currentFragmentContext
import com.mattworzala.canvas.internal.invokeFragment

//val TestA: FragmentContext
//    @Fragment get() = throw IllegalStateException("Implemented as intrinsic")

@Fragment
fun SayHello(name: String) {
    println("Hello, " + name)
}

@Fragment
fun TestFragment() {
    SayHello("world")
    SayHello("Michael")
    println("HELLO WORLD ")
    val b = currentFragmentContext
    println(b)
    currentFragmentContext.sayHello()
}

fun main() {
    invokeFragment(FragmentContext()) {
        TestFragment()
    }
}

//fun invokeFragment(context: FragmentContext, composable: @Fragment () -> Unit = {}) {
//    @Suppress("UNCHECKED_CAST")
//    val realFn = composable as Function2<FragmentContext, Int, Unit>
//    realFn(context, 1)
//}
"""

class CanvasIrTransformerTest {
    @Test
    fun `Playground`() {
        val result = compile(
            sourceFile = SourceFile.kotlin(
                "main.kt",
                testCode
            )
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val kClazz = result.classLoader.loadClass("MainKt")
        val main = kClazz.declaredMethods.single { it.name == "main" && it.parameterCount == 0 }
        main.invoke(null)
    }
}