package com.mattworzala.canvas.compiler.debuglog

import com.mattworzala.canvas.internal.currentFragmentContext
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
import com.mattworzala.canvas.internal.drawFragment

@Fragment
fun TestFragment() {
    println("HELLO WORLD")
}

fun main() {
    (TestFragment() as (FragmentContext) -> Unit)(FragmentContext())
//    drawFragment(FragmentContext(), ::TestFragment)
}
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