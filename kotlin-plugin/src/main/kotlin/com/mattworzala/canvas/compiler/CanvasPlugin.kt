package com.mattworzala.canvas.compiler

import com.google.auto.service.AutoService
import com.mattworzala.canvas.compiler.ir.DebugLogIrGenerationExtension
import com.mattworzala.canvas.compiler.ir.DebugLogTransformer
import com.mattworzala.canvas.compiler.ir.TemplateIrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.codegen.ClassBuilderFactory
import org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.com.intellij.openapi.extensions.impl.ExtensionPointImpl
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import org.jetbrains.kotlin.resolve.BindingContext

@AutoService(ComponentRegistrar::class)
class CanvasPlugin : ComponentRegistrar {
    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        // Can return here to not enable the plugin

        val messageCollector = configuration.get(
            CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY,
            MessageCollector.NONE)

//        messageCollector.report(CompilerMessageSeverity.LOGGING, "I AM A MESSAGE", CompilerMessageLocation.create(null))
//        ExpressionCodegenExtension.registerExtension(project,
//            CanvasCodegenExtension(messageCollector))



    //        println("REEEEEEE")
//        project.logError(RuntimeException("I AM ENABLED!!!"), PluginId.getId("canvas-compiler-plugin"))

//        ClassBuilderInterceptorExtension.registerExtension(project, object : ClassBuilderInterceptorExtension {
//            override fun interceptClassBuilderFactory(
//                interceptedFactory: ClassBuilderFactory,
//                bindingContext: BindingContext,
//                diagnostics: DiagnosticSink
//            ): ClassBuilderFactory {
//
//                return interceptedFactory
//            }
//        })


        IrGenerationExtension.registerExtension(project, DebugLogIrGenerationExtension())

//        IrGenerationExtension.registerExtension(project, TemplateIrGenerationExtension(messageCollector))

    }
}

internal fun <T : Any> ProjectExtensionDescriptor<T>.registerExtensionAsFirst(
    project: Project,
    extension: T
) {
    project.extensionArea
        .getExtensionPoint(extensionPointName)
        .let { it as ExtensionPointImpl }
        .registerExtension(extension, project)
}