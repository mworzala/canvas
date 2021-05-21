package com.mattworzala.canvas.compiler.ir

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import kotlin.math.exp

class FragmentTransformer(
    private val pluginContext: IrPluginContext
) : IrElementTransformerVoidWithContext() {
    private val annotationFragment = pluginContext.referenceClass(FqName("com.mattworzala.canvas.Fragment"))!!
    private val classFragmentContext = pluginContext.referenceClass(FqName("com.mattworzala.canvas.FragmentContext"))!!

    override fun visitFunctionNew(declaration: IrFunction): IrStatement {
        println("FUNCTION")

        var pe: PsiElement
        if (!declaration.hasAnnotation(annotationFragment))
            return super.visitFunctionNew(declaration)

        declaration.addValueParameter {
            name = Name.identifier("currentFragmentContext")
            type = classFragmentContext.defaultType
        }//("currentFragmentContext", classFragmentContext.defaultType, IrDeclarationOrigin.DEFINED)

//        println("I HAVE ANNOTATION")
        println(declaration.parent.dump())

        return super.visitFunctionNew(declaration)
    }

    override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
        val type = expression.type
        if (!type.hasAnnotation(annotationFragment))
            return super.visitFunctionReference(expression)

        println("FUNC HAS ANNOTATION")
        println(expression.dump())

        return super.visitFunctionReference(expression)
    }
}