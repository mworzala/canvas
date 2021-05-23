package com.mattworzala.canvas.compiler.fragment

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.types.addAnnotations
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

fun IrModuleFragment.annotateFragmentFunctions(pluginContext: IrPluginContext):
        Collection<IrAttributeContainer> {
    return IrFragmentAnnotator(pluginContext)
        .apply { this@annotateFragmentFunctions.acceptChildrenVoid(this) }
        .fragments
}

class IrFragmentAnnotator(val pluginContext: IrPluginContext) : IrElementVisitorVoid {
    val fragments = mutableSetOf<IrAttributeContainer>()
    val expectedFragments = mutableMapOf<IrElement, Boolean>()
    val expectedReturnFragment = mutableMapOf<FunctionDescriptor, Boolean>()

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun visitFunction(declaration: IrFunction) {
        expectedReturnFragment.put(
            declaration.descriptor,
            declaration.returnType.toKotlinType().hasFragmentAnnotation()
        )

        if (expectedFragments.get(declaration) == true) {
            declaration.setFragmentAnnotation()
        }

        if (declaration.hasFragmentAnnotation())
            fragments.add((declaration as IrAttributeContainer).attributeOwnerId)

        super.visitFunction(declaration)
    }

    override fun visitFunctionExpression(expression: IrFunctionExpression) {
        println("RMP ${expression.dump()}")

        if (expectedFragments.get(expression) == true) {
            val fragmentAnnotation = pluginContext.referenceClass(FragmentFqNames.Fragment)!!.owner
            expression.type = expression.type.addAnnotations(
                listOf(
                    IrConstructorCallImpl.fromSymbolOwner(
                        type = fragmentAnnotation.defaultType,
                        constructorSymbol = fragmentAnnotation.constructors.first().symbol
                    )
                )
            )
        }

        super.visitFunctionExpression(expression)
    }

    private fun IrFunction.setFragmentAnnotation() {
        if (hasFragmentAnnotation()) return
        val fragmentAnnotation = pluginContext.referenceClass(FragmentFqNames.Fragment)!!.owner
        annotations = annotations + listOf(
            IrConstructorCallImpl.fromSymbolOwner(
                type = fragmentAnnotation.defaultType,
                constructorSymbol = fragmentAnnotation.constructors.first().symbol
            )
        )
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun visitField(declaration: IrField) {
        declaration.initializer?.let { initializer ->
            expectedFragments.put(
                initializer,
                declaration.type.toKotlinType().hasFragmentAnnotation()
            )
        }
        super.visitField(declaration)
    }

    override fun visitCall(expression: IrCall) {
        val declaration = expression.symbol.owner
        if (declaration.hasFragmentAnnotation())
            fragments.add(declaration.attributeOwnerId)

        val irFunction = expression.symbol.owner
        irFunction.valueParameters.forEachIndexed { index, it ->
            val arg = expression.getValueArgument(index)
            if (arg != null) {
                val parameter = it.type.substitute(expression.typeSubstitutionMap)
                val isFragment = parameter.hasFragmentAnnotation()
                expectedFragments[arg] = isFragment
            }
        }
        super.visitCall(expression)
    }

    override fun visitConstructorCall(expression: IrConstructorCall) {
        var irFunction = expression.symbol.owner
        irFunction.valueParameters.forEachIndexed { index, it ->
            val arg = expression.getValueArgument(index)
            if (arg != null) {
                val parameter = it.type.substitute(expression.typeSubstitutionMap)
                val isFragment = parameter.hasFragmentAnnotation()
                expectedFragments[arg] = isFragment
            }
        }
        super.visitConstructorCall(expression)
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun visitValueParameter(declaration: IrValueParameter) {
        declaration.defaultValue?.let { defaultValue ->
            expectedFragments.put(
                defaultValue,
                declaration.type.toKotlinType().hasFragmentAnnotation()
            )
        }
        super.visitValueParameter(declaration)
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun visitExpression(expression: IrExpression) {
        val expectedType = expectedFragments.get(expression)
        when (expression) {
            is IrFunctionExpression ->
                expectedFragments.put(
                    expression.function,
                    expression.type.hasFragmentAnnotation()
                )
            is IrExpressionBody ->
                if (expectedType != null)
                    expectedFragments.put(expression.expression, expectedType)
            is IrReturn -> {
                val expectedReturnType = expectedReturnFragment.get(
                    expression.returnTargetSymbol.descriptor
                ) ?: false
                expectedFragments.put(expression.value, expectedReturnType)
            }
            is IrVararg -> {
                expression.elements.forEach {
                    expectedFragments.put(it, expression.type.hasFragmentAnnotation())
                }
            }
        }
        super.visitExpression(expression)
    }

    override fun visitWhen(expression: IrWhen) {
        val expectedType = expectedFragments.get(expression)
        if (expectedType != null)
            expression.branches.forEach {
                expectedFragments.put(it.result, expectedType)
            }
        super.visitWhen(expression)
    }

    override fun visitBody(body: IrBody) {
        val expectedType = expectedFragments.get(body)
        when (body) {
            is IrExpressionBody ->
                if (expectedType != null)
                    expectedFragments.put(body.expression, expectedType)
        }
        super.visitBody(body)
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun visitDeclaration(declaration: IrDeclarationBase) {
        when (declaration) {
            is IrProperty -> {
                declaration.getter?.let { getter ->
                    expectedFragments.put(getter, getter.descriptor.isMarkedAsFragment())
                }
                declaration.setter?.let { setter ->
                    expectedFragments.put(setter, setter.descriptor.isMarkedAsFragment())
                }
            }
        }
        super.visitDeclaration(declaration)
    }

    override fun visitContainerExpression(expression: IrContainerExpression) {
        val expectedType = expectedFragments.get(expression)
        if (expectedType != null && expression.statements.size > 0)
            expectedFragments.put(
                expression.statements.last(),
                expectedType
            )
        super.visitContainerExpression(expression)
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }
}