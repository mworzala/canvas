package com.mattworzala.canvas.compiler.fragment

import com.mattworzala.canvas.compiler.lower.ModuleLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.isLambda
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class FragmentFunInterfaceLowering(private val context: IrPluginContext) :
    IrElementTransformerVoidWithContext(),
    ModuleLoweringPass {

    override fun lower(module: IrModuleFragment) = module.transformChildrenVoid(this)

    private fun isFunInterfaceConversion(expression: IrTypeOperatorCall): Boolean {
        val argument = expression.argument
        val operator = expression.operator
        val type = expression.typeOperand
        val functionClass = type.classOrNull
        return operator == IrTypeOperator.SAM_CONVERSION &&
                argument is IrFunctionExpression &&
                argument.origin.isLambda &&
                functionClass != null &&
                functionClass.owner.isFun
        // IMPORTANT(b/178663739):
        // We are transforming not just SAM conversions for fragment fun interfaces, but ALL
        // fun interfaces temporarily until KT-44622 gets fixed in the version of kotlin we
        // are using, which should be in 1.4.30.
        // Once it does, we should either add the below additional condition to this predicate,
        // or, if possible, remove this lowering all together if kotlin's lowering works for
        // composable fun interfaces as well.
        //
        // functionClass.functions.single {
        //    it.owner.modality == Modality.ABSTRACT
        // }.owner.annotations.hasAnnotation(FragmentFqNames.FRAGMENT)
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
        if (isFunInterfaceConversion(expression)) {
            val argument = expression.argument.transform(this, null) as IrFunctionExpression
            val superType = expression.typeOperand
            val superClass = superType.classOrNull ?: error("Expected non-null class")
            return FunctionReferenceBuilder(
                argument,
                superClass,
                superType,
                currentDeclarationParent!!,
                context,
                currentScope!!.scope.scopeOwnerSymbol,
                context.irBuiltIns
            ).build()
        }
        return super.visitTypeOperator(expression)
    }
}
