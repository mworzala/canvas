package com.mattworzala.canvas.compiler

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration

@AutoService(CommandLineProcessor::class)
class CanvasCommandLineProcessor : CommandLineProcessor {
    override val pluginId: String = "canvas-compiler-plugin"

    override val pluginOptions: Collection<AbstractCliOption> = listOf()

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        throw IllegalArgumentException("Unexpected config option ${option.optionName}")
    }
}