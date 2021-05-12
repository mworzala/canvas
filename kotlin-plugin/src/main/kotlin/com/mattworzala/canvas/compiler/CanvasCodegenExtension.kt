package com.mattworzala.canvas.compiler

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.codegen.ImplementationBodyCodegen
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension

internal const val LOG_PREFIX = "*** CANVAS"

class CanvasCodegenExtension(
    private val messageCollector: MessageCollector
) : ExpressionCodegenExtension {
    private fun log(message: String) {
        messageCollector.report(
            LOGGING,
            "$LOG_PREFIX $message",
            CompilerMessageLocation.create(null))
    }

    override val shouldGenerateClassSyntheticPartsInLightClassesMode: Boolean = true

    override fun generateClassSyntheticParts(codegen: ImplementationBodyCodegen) {
        val targetClass = codegen.descriptor
        log("Reading ${targetClass.name}")

        messageCollector.report(ERROR, "Test is a blah blah!", null)
    }
}