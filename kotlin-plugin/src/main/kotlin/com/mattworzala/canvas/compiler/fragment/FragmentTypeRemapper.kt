package com.mattworzala.canvas.compiler.fragment

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContextImpl
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrTypeAbbreviationImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance

class FragmentTypeRemapper(
    private val context: IrPluginContext,
    private val symbolRemapper: SymbolRemapper,
    private val typeTranslator: TypeTranslator,
    private val fragmentType: IrType
) : TypeRemapper {
    lateinit var deepCopy: IrElementTransformerVoid

    private val scopeStack = mutableListOf<IrTypeParametersContainer>()

    override fun enterScope(irTypeParametersContainer: IrTypeParametersContainer) {
        scopeStack.add(irTypeParametersContainer)
    }

    override fun leaveScope() {
        scopeStack.pop()
    }

    private fun IrType.isFragment(): Boolean =
        annotations.hasAnnotation(FragmentFqNames.Fragment)

    private val IrConstructorCall.annotationClass
        get() = this.symbol.owner.returnType.classifierOrNull

    private fun List<IrConstructorCall>.hasAnnotation(fqName: FqName): Boolean =
        any { it.annotationClass?.isClassWithFqName(fqName.toUnsafe()) ?: false }

    private fun KotlinType.toIrType(): IrType = typeTranslator.translateType(this)

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun IrType.isFunction(): Boolean {
        val classifier = classifierOrNull ?: return false
        val name = classifier.descriptor.name.asString()
        if (!name.startsWith("Function")) return false
        classifier.descriptor.name
        return true
    }

    override fun remapType(type: IrType): IrType {
        println("REMAPPING TYPE " + type.dumpKotlinLike())
        if (type !is IrSimpleType) return type
        println("RT.0")
        if (!type.isFunction()) return underlyingRemapType(type)
        println("RT.1")
        if (!type.isFragment()) return underlyingRemapType(type)
        println("RT.2")

        val oldIrArguments = type.arguments
        val realParams = oldIrArguments.size - 1
        var extraArgs = listOf(
            // fragment param
            makeTypeProjection(
                fragmentType,
                Variance.INVARIANT
            )
        )
        val changedParams = changedParamCount(realParams, 1)
        extraArgs = extraArgs + (0 until changedParams).map {
            makeTypeProjection(context.irBuiltIns.intType, Variance.INVARIANT)
        }
        val newIrArguments = oldIrArguments
            .subList(0, oldIrArguments.size - 1) + extraArgs + oldIrArguments.last()

        val newArgSize = oldIrArguments.size - 1 + extraArgs.size
        val functionCls = context.function(newArgSize)

        return IrSimpleTypeImpl(
            null,
            functionCls,
            type.hasQuestionMark,
            newIrArguments.map { remapTypeArgument(it) },
            type.annotations.filter { !it.isFragmentAnnotation() }.map {
                it.transform(deepCopy, null) as IrConstructorCall
            },
            null
        )
    }

    private fun underlyingRemapType(type: IrSimpleType): IrType {
        return IrSimpleTypeImpl(
            null,
            symbolRemapper.getReferencedClassifier(type.classifier),
            type.hasQuestionMark,
            type.arguments.map { remapTypeArgument(it) },
            type.annotations.map { it.transform(deepCopy, null) as IrConstructorCall },
            type.abbreviation?.remapTypeAbbreviation()
        )
    }

    private fun remapTypeArgument(typeArgument: IrTypeArgument): IrTypeArgument =
        if (typeArgument is IrTypeProjection)
            makeTypeProjection(this.remapType(typeArgument.type), typeArgument.variance)
        else
            typeArgument

    private fun IrTypeAbbreviation.remapTypeAbbreviation() =
        IrTypeAbbreviationImpl(
            symbolRemapper.getReferencedTypeAlias(typeAlias),
            hasQuestionMark,
            arguments.map { remapTypeArgument(it) },
            annotations
        )
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
private fun IrConstructorCall.isFragmentAnnotation() =
    this.symbol.descriptor.returnType.constructor.declarationDescriptor?.fqNameSafe ==
            FragmentFqNames.Fragment

class DeepCopyIrTreeWithSymbolsPreservingMetadata(
    private val context: IrPluginContext,
    private val symbolRemapper: DeepCopySymbolRemapper,
    private val typeRemapper: TypeRemapper,
    private val typeTranslator: TypeTranslator,
    symbolRenamer: SymbolRenamer = SymbolRenamer.DEFAULT
) : DeepCopyIrTreeWithSymbols(symbolRemapper, typeRemapper, symbolRenamer) {

    override fun visitClass(declaration: IrClass): IrClass {
        return super.visitClass(declaration).also { it.copyMetadataFrom(declaration) }
    }

    override fun visitFunction(declaration: IrFunction): IrStatement {
        return super.visitFunction(declaration).also {
            it.copyMetadataFrom(declaration)
        }
    }

    override fun visitConstructor(declaration: IrConstructor): IrConstructor {
        return super.visitConstructor(declaration).also {
            WrappedFragmentDescriptorPatcher.visitConstructor(it)

            it.copyMetadataFrom(declaration)
        }
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrSimpleFunction {
        if (declaration.symbol.isRemappedAndBound()) {
            return symbolRemapper.getReferencedSimpleFunction(declaration.symbol).owner
        }
        if (declaration.symbol.isBoundButNotRemapped()) {
            symbolRemapper.visitSimpleFunction(declaration)
        }
        return super.visitSimpleFunction(declaration).also {
            WrappedFragmentDescriptorPatcher.visitSimpleFunction(it)

            it.correspondingPropertySymbol = declaration.correspondingPropertySymbol
            it.copyMetadataFrom(declaration)
        }
    }

    override fun visitField(declaration: IrField): IrField {
        return super.visitField(declaration).also {
            it.metadata = declaration.metadata
        }
    }

    override fun visitProperty(declaration: IrProperty): IrProperty {
        return super.visitProperty(declaration).also {
            it.copyMetadataFrom(declaration)
            it.copyAttributes(declaration)
        }
    }

    override fun visitFile(declaration: IrFile): IrFile {
        return super.visitFile(declaration).also {
            if (it is IrFileImpl) {
                it.metadata = declaration.metadata
            }
        }
    }

    override fun visitConstructorCall(expression: IrConstructorCall): IrConstructorCall {
        if (!expression.symbol.isBound)
            (context as IrPluginContextImpl).linker.getDeclaration(expression.symbol)
        val ownerFn = expression.symbol.owner as? IrConstructor
        // If we are calling an external constructor, we want to "remap" the types of its signature
        // as well, since if it they are @Fragment it will have its unmodified signature. These
        // types won't be traversed by default by the DeepCopyIrTreeWithSymbols so we have to
        // do it ourself here.
        if (
            ownerFn != null &&
            ownerFn.origin == IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB
        ) {
            symbolRemapper.visitConstructor(ownerFn)
            val newFn = super.visitConstructor(ownerFn).also {
                it.parent = ownerFn.parent
                it.patchDeclarationParents(it.parent)
            }
            val newCallee = symbolRemapper.getReferencedConstructor(newFn.symbol)

            return IrConstructorCallImpl(
                expression.startOffset, expression.endOffset,
                expression.type.remapType(),
                newCallee,
                expression.typeArgumentsCount,
                expression.constructorTypeArgumentsCount,
                expression.valueArgumentsCount,
                mapStatementOrigin(expression.origin)
            ).apply {
                copyRemappedTypeArgumentsFrom(expression)
                transformValueArguments(expression)
            }.copyAttributes(expression)
        }
        return super.visitConstructorCall(expression)
    }

    private fun IrFunction.hasFragmentArguments(): Boolean {
        if (
            dispatchReceiverParameter?.type?.isFragment() == true ||
            extensionReceiverParameter?.type?.isFragment() == true
        ) return true

        for (param in valueParameters) {
            if (param.type.isFragment()) return true
        }
        return false
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun visitCall(expression: IrCall): IrCall {
        val ownerFn = expression.symbol.owner as? IrSimpleFunction
        @Suppress("DEPRECATION")
        val containingClass = expression.symbol.descriptor.containingDeclaration as? ClassDescriptor

        // Any virtual calls on fragment functions we want to make sure we update the call to
        // the right function base class (of n+1 arity). The most often virtual call to make on
        // a function instance is `invoke`, which we *already* do in the FragmentParamTransformer.
        // There are others that can happen though as well, such as `equals` and `hashCode`. In this
        // case, we want to update those calls as well.
        if (
            ownerFn != null &&
            ownerFn.origin == IrDeclarationOrigin.FAKE_OVERRIDE &&
            containingClass != null &&
            containingClass.defaultType.isFunctionType &&
            expression.dispatchReceiver?.type?.isFragment() == true
        ) {
            val typeArguments = containingClass.defaultType.arguments
            val newFnClass = context.function(typeArguments.size).owner

            var newFn = newFnClass
                .functions
                .first { it.name == ownerFn.name }

            symbolRemapper.visitSimpleFunction(newFn)
            newFn = super.visitSimpleFunction(newFn).also { fn ->
                fn.parent = newFnClass
                fn.overriddenSymbols = ownerFn.overriddenSymbols.map { it }
                fn.dispatchReceiverParameter = ownerFn.dispatchReceiverParameter
                fn.extensionReceiverParameter = ownerFn.extensionReceiverParameter
                newFn.valueParameters.forEach { p ->
                    fn.addValueParameter(p.name.identifier, p.type)
                }
                fn.patchDeclarationParents(fn.parent)
                assert(fn.body == null) { "expected body to be null" }
            }

            val newCallee = symbolRemapper.getReferencedSimpleFunction(newFn.symbol)
            return shallowCopyCall(expression, newCallee).apply {
                copyRemappedTypeArgumentsFrom(expression)
                transformValueArguments(expression)
            }
        }

        // If we are calling an external function, we want to "remap" the types of its signature
        // as well, since if it is @Fragment it will have its unmodified signature. These
        // functions won't be traversed by default by the DeepCopyIrTreeWithSymbols so we have to
        // do it ourself here.
        //
        // When an external declaration for a property getter/setter is transformed, we need to
        // also transform the corresponding property so that we maintain the relationship
        // `getterFun.correspondingPropertySymbol.owner.getter == getterFun`. If we do not
        // maintain this relationship inline class getters will be incorrectly compiled.
        if (
            ownerFn != null &&
            ownerFn.origin == IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB
        ) {
            if (ownerFn.correspondingPropertySymbol != null) {
                val property = ownerFn.correspondingPropertySymbol!!.owner
                symbolRemapper.visitProperty(property)
                visitProperty(property).also {
                    it.getter?.correspondingPropertySymbol = it.symbol
                    it.setter?.correspondingPropertySymbol = it.symbol
                    it.parent = ownerFn.parent
                    it.patchDeclarationParents(it.parent)
                    it.copyAttributes(property)
                }
            } else {
                symbolRemapper.visitSimpleFunction(ownerFn)
                visitSimpleFunction(ownerFn).also {
                    it.parent = ownerFn.parent
                    it.correspondingPropertySymbol = null
                    it.patchDeclarationParents(it.parent)
                }
            }
            val newCallee = symbolRemapper.getReferencedSimpleFunction(ownerFn.symbol)
            return shallowCopyCall(expression, newCallee).apply {
                copyRemappedTypeArgumentsFrom(expression)
                transformValueArguments(expression)
            }
        }

        if (
            ownerFn != null &&
            ownerFn.hasFragmentArguments()
        ) {
            val newFn = visitSimpleFunction(ownerFn).also {
                it.overriddenSymbols = ownerFn.overriddenSymbols.map { override ->
                    if (override.isBound) {
                        visitSimpleFunction(override.owner).apply {
                            parent = override.owner.parent
                        }.symbol
                    } else {
                        override
                    }
                }
                it.parent = ownerFn.parent
                it.patchDeclarationParents(it.parent)
            }
            val newCallee = symbolRemapper.getReferencedSimpleFunction(newFn.symbol)
            return shallowCopyCall(expression, newCallee).apply {
                copyRemappedTypeArgumentsFrom(expression)
                transformValueArguments(expression)
            }
        }

        return super.visitCall(expression)
    }

    private fun IrSimpleFunctionSymbol.isBoundButNotRemapped(): Boolean {
        return this.isBound && symbolRemapper.getReferencedFunction(this) == this
    }

    private fun IrSimpleFunctionSymbol.isRemappedAndBound(): Boolean {
        val symbol = symbolRemapper.getReferencedFunction(this)
        return symbol.isBound && symbol != this
    }

    /* copied verbatim from DeepCopyIrTreeWithSymbols, except with newCallee as a parameter */
    private fun shallowCopyCall(expression: IrCall, newCallee: IrSimpleFunctionSymbol): IrCall {
        return IrCallImpl(
            expression.startOffset, expression.endOffset,
            expression.type.remapType(),
            newCallee,
            expression.typeArgumentsCount,
            expression.valueArgumentsCount,
            mapStatementOrigin(expression.origin),
            symbolRemapper.getReferencedClassOrNull(expression.superQualifierSymbol)
        ).apply {
            copyRemappedTypeArgumentsFrom(expression)
        }.copyAttributes(expression)
    }

    /* copied verbatim from DeepCopyIrTreeWithSymbols */
    private fun IrMemberAccessExpression<*>.copyRemappedTypeArgumentsFrom(
        other: IrMemberAccessExpression<*>
    ) {
        assert(typeArgumentsCount == other.typeArgumentsCount) {
            "Mismatching type arguments: $typeArgumentsCount vs ${other.typeArgumentsCount} "
        }
        for (i in 0 until typeArgumentsCount) {
            putTypeArgument(i, other.getTypeArgument(i)?.remapType())
        }
    }

    /* copied verbatim from DeepCopyIrTreeWithSymbols */
    private fun <T : IrMemberAccessExpression<*>> T.transformValueArguments(original: T) {
        transformReceiverArguments(original)
        for (i in 0 until original.valueArgumentsCount) {
            putValueArgument(i, original.getValueArgument(i)?.transform())
        }
    }

    /* copied verbatim from DeepCopyIrTreeWithSymbols */
    private fun <T : IrMemberAccessExpression<*>> T.transformReceiverArguments(original: T): T =
        apply {
            dispatchReceiver = original.dispatchReceiver?.transform()
            extensionReceiver = original.extensionReceiver?.transform()
        }

    private fun IrElement.copyMetadataFrom(owner: IrMetadataSourceOwner) {
        if (this is IrMetadataSourceOwner) {
            metadata = owner.metadata
        } else {
            throw IllegalArgumentException("Cannot copy metadata to $this")
        }
    }

    private fun IrType.isFragment(): Boolean {
        return annotations.hasAnnotation(FragmentFqNames.Fragment)
    }

    private fun KotlinType.toIrType(): IrType = typeTranslator.translateType(this)
}