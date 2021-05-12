package com.mattworzala.canvas.compiler.ir

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class TemplateIrGenerationExtension(
    private val messageCollector: MessageCollector
) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val typeNullableAny = pluginContext.irBuiltIns.anyNType
        val typeUnit = pluginContext.irBuiltIns.unitType

        val funPrintln = pluginContext.referenceFunctions(FqName("kotlin.io.println"))
            .single {
                val parameters = it.owner.valueParameters
                parameters.size == 1 && parameters[0].type == typeNullableAny
            }

        val funMain = pluginContext.irFactory.buildFun {
            name = Name.identifier("main")
            visibility = DescriptorVisibilities.PUBLIC
            modality = Modality.FINAL
            returnType = typeUnit
        }
        funMain.body = DeclarationIrBuilder(pluginContext, funMain.symbol).irBlockBody {
            val callPrintln = irCall(funPrintln)
            callPrintln.putValueArgument(0, irString("Hello, World!"))
            +callPrintln
        }

        println(funMain.dump())
//        TODO("Not yet implemenrted")
    }
}