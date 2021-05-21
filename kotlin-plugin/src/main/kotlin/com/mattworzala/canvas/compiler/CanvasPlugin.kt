package com.mattworzala.canvas.compiler

import com.google.auto.service.AutoService
import com.mattworzala.canvas.compiler.debuglog.DebugLogIrGenerationExtension
import com.mattworzala.canvas.compiler.fragment.FragmentCallChecker
import com.mattworzala.canvas.compiler.fragment.FragmentIrGenerationExtension
import com.mattworzala.canvas.compiler.ir.CanvasIrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor


//https://android.googlesource.com/platform/frameworks/support/+/refs/heads/androidx-main/compose/compiler/compiler-hosted/src/main/resources/META-INF/services/org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages%24Extension
@AutoService(ComponentRegistrar::class)
class CanvasPlugin : ComponentRegistrar {
    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        // Can return here to not enable the plugin

        StorageComponentContainerContributor.registerExtension(
            project,
            FragmentCallChecker()
        )
        //todo FragmentDeclarationChecker

        IrGenerationExtension.registerExtension(project,
            FragmentIrGenerationExtension())



//        IrGenerationExtension.registerExtension(project,
//            CanvasIrGenerationExtension(messageCollector))
    }
}
