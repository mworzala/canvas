package com.mattworzala.canvas.compiler.fragment

import com.mattworzala.canvas.compiler.lower.ModuleLoweringPass
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ContextIntrinsicTransformer(
    val context: IrPluginContext
) : IrElementTransformerVoid(), FileLoweringPass, ModuleLoweringPass {
//    private val currentContextIntrinsic = FqName.ROOT.child(Name.identifier("<get-TestA>"))
    private val currentContextIntrinsic = FragmentFqNames.ContextIntrinsic

    override fun lower(module: IrModuleFragment) {
        module.transformChildrenVoid(this)
    }

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun visitCall(expression: IrCall): IrExpression {
        val calleeFqName = expression.symbol.descriptor.fqNameSafe
        if (calleeFqName == currentContextIntrinsic) {
            // since this call was transformed by the FragmentParamTransformer, the first argument
            // to this call is the fragment itself. We just replace this expression with the
            // argument expression and we are good.
            val expectedArgumentsCount = 1 + // fragment parameter
                                         1 // changed parameter
            assert(expression.valueArgumentsCount == expectedArgumentsCount) {
                """
                    Fragment Context call doesn't match expected argument count:
                        expected: $expectedArgumentsCount,
                        actual: ${expression.valueArgumentsCount},
                        expression: ${expression.dump()}
                """.trimIndent()
            }
            val contextExpr = expression.getValueArgument(0) ?: error("Expected non-null fragment context argument")
            return contextExpr
        }
        return super.visitCall(expression)
    }
}