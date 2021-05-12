package com.mattworzala.canvas.compiler

import com.google.auto.service.AutoService
import com.mattworzala.canvas.compiler.debuglog.DebugLogIrGenerationExtension
import com.mattworzala.canvas.compiler.ir.CanvasIrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration

@AutoService(ComponentRegistrar::class)
class CanvasPlugin : ComponentRegistrar {
    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        // Can return here to not enable the plugin

        val messageCollector = configuration.get(
            CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY,
            MessageCollector.NONE)

        IrGenerationExtension.registerExtension(project,
            CanvasIrGenerationExtension(messageCollector))
    }
}
