package com.mattworzala.canvas.compiler.debuglog

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import compile
import kotlin.test.assertEquals
import org.junit.Test

class TemplateIrGenerationExtensionTest {
    @Test
    fun `IR plugin success`() {
        val result = compile(
            sourceFile = SourceFile.kotlin(
                "main.kt", """
                annotation class DebugLog                   
                
                fun main() {
                    println(greet())
                    println(greet(name = "Kotlin IR"))
                }
                      
                @DebugLog
                fun greet(greeting: String = "Hello", name: String = "World"): String { 
                    Thread.sleep(15) // simulate work
                    return "${'$'}greeting, ${'$'}name!"
                }
                """.trimIndent()
            )
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val kClazz = result.classLoader.loadClass("MainKt")
        val main = kClazz.declaredMethods.single { it.name == "main" && it.parameterCount == 0 }
        main.invoke(null)
    }
}
