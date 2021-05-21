package com.mattworzala.canvas.compiler.lower

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.jvm.ir.isInlineParameter
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.isLambda
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

internal open class IrInlineReferenceLocator(private val context: IrPluginContext) :
    IrElementVisitor<Unit, IrDeclaration?> {
    override fun visitElement(element: IrElement, data: IrDeclaration?) {
        element.acceptChildren(this, data)
    }

    override fun visitDeclaration(declaration: IrDeclarationBase, data: IrDeclaration?) {
        val scope = if (declaration is IrVariable) data else declaration
        declaration.acceptChildren(this, scope)
    }

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression, data: IrDeclaration?) {
        val function = expression.symbol.owner
        if (function.isInlineFunctionCall(context)) {
            for (parameter in function.valueParameters) {
                if (!parameter.isInlineParameter())
                    continue

                val valueArgument = expression.getValueArgument(parameter.index) ?: continue
                if (!valueArgument.isInlineIrExpression())
                    continue

                if (valueArgument is IrBlock) {
                    visitInlineLambda(
                        valueArgument.statements.last() as IrFunctionReference,
                        function,
                        parameter,
                        data!!
                    )
                } else if (valueArgument is IrFunctionExpression) {
                    visitInlineLambda(valueArgument, function, parameter, data!!)
                } else if (valueArgument is IrCallableReference<*>) {
                    visitInlineReference(valueArgument)
                }
            }
        }
        return super.visitFunctionAccess(expression, data)
    }

    open fun visitInlineReference(argument: IrCallableReference<*>) {}

    open fun visitInlineLambda(
        argument: IrFunctionReference,
        callee: IrFunction,
        parameter: IrValueParameter,
        scope: IrDeclaration
    ) = visitInlineReference(argument)

    open fun visitInlineLambda(
        argument: IrFunctionExpression,
        callee: IrFunction,
        parameter: IrValueParameter,
        scope: IrDeclaration
    ) {}

    companion object {
        fun scan(context: IrPluginContext, element: IrElement): Set<InlineLambdaInfo> =
            mutableSetOf<InlineLambdaInfo>().apply {
                element.accept(
                    object : IrInlineReferenceLocator(context) {
                        override fun visitInlineLambda(
                            argument: IrFunctionExpression,
                            callee: IrFunction,
                            parameter: IrValueParameter,
                            scope: IrDeclaration
                        ) {
                            add(InlineLambdaInfo(argument, callee, parameter, scope))
                        }
                    },
                    null
                )
            }
    }
}

data class InlineLambdaInfo(
    val argument: IrFunctionExpression,
    val callee: IrFunction,
    val parameter: IrValueParameter,
    val scope: IrDeclaration
)

@Suppress("UNUSED_PARAMETER")
fun IrFunction.isInlineFunctionCall(context: IrPluginContext) =
    (/*!context.state.isInlineDisabled */ true || typeParameters.any { it.isReified }) && isInline

fun IrExpression.isInlineIrExpression() =
    when (this) {
        is IrBlock -> origin.isInlineIrExpression()
        is IrCallableReference<*> -> true.also {
            assert((0 until valueArgumentsCount).count { getValueArgument(it) != null } == 0) {
                "Expecting 0 value arguments for bounded callable reference: ${dump()}"
            }
        }
        is IrFunctionExpression -> origin.isInlineIrExpression()
        else -> false
    }

fun IrStatementOrigin?.isInlineIrExpression(): Boolean {
    if (isLambda) return true
    if (this == IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE) return true
    if (this == IrStatementOrigin.SUSPEND_CONVERSION) return true
    return false
}
