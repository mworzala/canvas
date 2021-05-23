package com.mattworzala.canvas.compiler.fragment

import com.mattworzala.canvas.compiler.lower.ModuleLoweringPass
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.jvm.ir.isInlineParameter
import org.jetbrains.kotlin.backend.jvm.lower.inlineclasses.InlineClassAbi
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.descriptors.PropertySetterDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.util.OperatorNameConventions
import kotlin.math.min

class FragmentParamTransformer(
    context: IrPluginContext,
    symbolRemapper: DeepCopySymbolRemapper,
    bindingTrace: BindingTrace
) : AbstractFragmentLowering(context, symbolRemapper, bindingTrace), ModuleLoweringPass {
    /**
     * Used to identify module fragment in case of incremental compilation
     * see [externallyTransformed]
     */
    private var currentModule: IrModuleFragment? = null

    override fun lower(module: IrModuleFragment) {
        super.lower(module)
        currentModule = module

        module.transformChildrenVoid(this)

        module.acceptVoid(symbolRemapper)

        val typeRemapper = FragmentTypeRemapper(
            context,
            symbolRemapper,
            typeTranslator,
            fragmentType
        )

        // for each declaration, we create a deepCopy transformer It is important here that we
        // use the "preserving metadata" variant since we are using this copy to *replace* the
        // originals, or else the module we would produce wouldn't have any metadata in it.
        val transformer = DeepCopyIrTreeWithSymbolsPreservingMetadata(
            context,
            symbolRemapper,
            typeRemapper,
            typeTranslator
        ).also { typeRemapper.deepCopy = it }
        module.transformChildren(transformer, null)
        // just go through and patch all of the parents to make sure things are properly wired
        // up.
        module.patchDeclarationParents()
    }

    private val transformedFunctions: MutableMap<IrSimpleFunction, IrSimpleFunction> = mutableMapOf()

    private val transformedFunctionSet = mutableSetOf<IrFunction>()

    private val fragmentType = fragmentIrClass.defaultType.replaceArgumentsWithStarProjections()

    override fun visitFunction(declaration: IrFunction): IrStatement {
        val v1 = declaration.withFragmentParamIfNeeded()
        val v2 = super.visitFunction(v1)
        return v2
    }

    fun IrCall.withFragmentParamIfNeeded(fragmentParam: IrValueParameter): IrCall {
        println("ADDING FRAGMENT PARAM TO CALL: " + fragmentParam.dump())
        val isFragmentLambda = isFragmentLambdaInvoke()

        if (!symbol.owner.hasFragmentAnnotation() && !isFragmentLambda)
            return this
        val ownerFn = when {
            isFragmentLambda -> symbol.owner.lambdaInvokeWithFragmentParam()
            else -> symbol.owner.withFragmentParamIfNeeded()
        }

        //todo decoy
        // externally transformed functions are already remapped from decoys, so we only need to
        // add the parameters to the call
        if (!ownerFn.externallyTransformed()) {
            if (!isFragmentLambda && !transformedFunctionSet.contains(ownerFn))
                return this
            if (symbol.owner == ownerFn)
                return this
        }

        println("REPLACING CALL")

        return IrCallImpl(
            startOffset,
            endOffset,
            type,
            ownerFn.symbol as IrSimpleFunctionSymbol,
            typeArgumentsCount,
            ownerFn.valueParameters.size,
            origin,
            superQualifierSymbol
        ).also {
            it.copyAttributes(this)
            context.irTrace.record(
                FragmentWritableSlices.IS_FRAGMENT_CALL,
                it, true
            )
            it.copyTypeArgumentsFrom(this)
            it.dispatchReceiver = dispatchReceiver
            it.extensionReceiver = extensionReceiver
            //todo this is creating that missing arguments bitmap, need to figure out the rest of this block
            val argumentsMissing = mutableListOf<Boolean>()
            println(valueArgumentsCount)
            for (i in 0 until valueArgumentsCount) {
                val arg = getValueArgument(i)
                argumentsMissing.add(arg == null)
                if (arg != null) {
                    it.putValueArgument(i, arg)
                } else {
                    it.putValueArgument(i, defaultArgumentFor(ownerFn.valueParameters[i]))
                }
            }
            val realValueParams = valueArgumentsCount
            var argIndex = valueArgumentsCount
            it.putValueArgument(
                argIndex++,
                IrGetValueImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    fragmentParam.symbol
                )
            )

            //todo this handles the changed parameters bitmap
            // $changed[n]
            for (i in 0 until changedParamCount(realValueParams, ownerFn.thisParamCount)) {
                if (argIndex < ownerFn.valueParameters.size) {
                    it.putValueArgument(
                        argIndex++,
                        irConst(0)
                    )
                } else {
                    error("1. expected value parameter count to be higher: ${this.dumpSrc()}")
                }
            }

            // $default[n]
            for (i in 0 until defaultParamCount(realValueParams)) {
                val start = i * BITS_PER_INT
                val end = min(start + BITS_PER_INT, realValueParams)
                if (argIndex < ownerFn.valueParameters.size) {
                    val bits = argumentsMissing
                        .toBooleanArray()
                        .sliceArray(start until end)
                    it.putValueArgument(argIndex++, irConst(bitMask(*bits)))
                } else if (argumentsMissing.any { it }) {
                    error("2. expected value parameter count to be higher: ${this.dumpSrc()}")
                }
            }
        }
    }

    private fun defaultArgumentFor(param: IrValueParameter): IrExpression? {
        if (param.varargElementType != null) return null
        return param.type.defaultValue().let {
            IrCompositeImpl(
                it.startOffset,
                it.endOffset,
                it.type,
                IrStatementOrigin.DEFAULT_VALUE,
                listOf(it)
            )
        }
    }

    // TODO(lmr): There is an equivalent function in IrUtils, but we can't use it because it
    //  expects a JvmBackendContext. That implementation uses a special "unsafe coerce" builtin
    //  method, but we don't have access to that so instead we are just going to construct the
    //  inline class itself and hope that it gets lowered properly.
    private fun IrType.defaultValue(
        startOffset: Int = UNDEFINED_OFFSET,
        endOffset: Int = UNDEFINED_OFFSET
    ): IrExpression {
        val classSymbol = classOrNull
        if (this !is IrSimpleType || hasQuestionMark || classSymbol?.owner?.isInline != true) {
            return if (isMarkedNullable()) {
                IrConstImpl.constNull(startOffset, endOffset, context.irBuiltIns.nothingNType)
            } else {
                IrConstImpl.defaultValueForType(startOffset, endOffset, this)
            }
        }

        val klass = classSymbol.owner
        val ctor = classSymbol.constructors.first()
        val underlyingType = InlineClassAbi.getUnderlyingType(klass)

        // TODO(lmr): We should not be calling the constructor here, but this seems like a
        //  reasonable interim solution. We should figure out how to get access to the unsafe
        //  coerce and use that instead if possible.
        return IrConstructorCallImpl(
            startOffset,
            endOffset,
            this,
            ctor,
            typeArgumentsCount = 0,
            constructorTypeArgumentsCount = 0,
            valueArgumentsCount = 1,
            origin = null
        ).also {
            it.putValueArgument(0, underlyingType.defaultValue(startOffset, endOffset))
        }
    }

    // Transform `@Fragment fun foo(params): RetType` into `fun foo(params, $fragment: Fragment): RetType`
    private fun IrFunction.withFragmentParamIfNeeded(): IrFunction {
        println("WITH PARAM IF NEEDED ${this.dump()}")
        // don't transform functions that themselves were produced by this function. (ie, if we
        // call this with a function that has the synthetic composer parameter, we don't want to
        // transform it further).
        if (transformedFunctionSet.contains(this)) return this
        println("WPIF - 0")

        // some functions were transformed during previous compilations or in other modules
        if (this.externallyTransformed()) {
            return this
        }
        println("WPIF - 1")

        // if not a composable fn, nothing to do
        if (!this.hasFragmentAnnotation()) {
            return this
        }
        println("WPIF - 2")

        // if this function is an inlined lambda passed as an argument to an inline function (and
        // is NOT a composable lambda), then we don't want to transform it. Ideally, this
        // wouldn't have gotten this far because the `isComposable()` check above should return
        // false, but right now the composable annotation checker seems to produce a
        // false-positive here. It is important that we *DO NOT* transform this, but we should
        // probably fix the annotation checker instead.
        // TODO is this still an issue? add a test :)
        if (isNonFragmentInlinedLambda()) return this
        println("WPIF - 3")

        // we don't bother transforming expect functions. They exist only for type resolution and
        // don't need to be transformed to have a composer parameter
        if (isExpect) return this
        println("WPIF - 4")

        // cache the transformed function with composer parameter
        return transformedFunctions[this] ?: copyWithFragmentParam()
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun IrFunction.lambdaInvokeWithFragmentParam(): IrFunction {
        val descriptor = descriptor
        val argCount = descriptor.valueParameters.size
        val extraParams = fragmentSyntheticParamCount(argCount, hasDefaults = false)
        val newFnClass = context.function(argCount + extraParams).owner
        return newFnClass.functions.first {
            it.name == OperatorNameConventions.INVOKE
        }
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun IrFunction.copy(
        isInline: Boolean = this.isInline,
        modality: Modality = descriptor.modality
    ): IrSimpleFunction {
        // TODO(lmr): use deepCopy instead?
        val descriptor = descriptor

        return IrFunctionImpl(
            startOffset,
            endOffset,
            origin,
            IrSimpleFunctionSymbolImpl(),
            name,
            visibility,
            modality,
            returnType,
            isInline,
            isExternal,
            descriptor.isTailrec,
            descriptor.isSuspend,
            descriptor.isOperator,
            descriptor.isInfix,
            isExpect,
            isFakeOverride,
            containerSource
        ).also { fn ->
            if (this is IrSimpleFunction) {
                val propertySymbol = correspondingPropertySymbol
                if (propertySymbol != null) {
                    fn.correspondingPropertySymbol = propertySymbol
                    if (propertySymbol.owner.getter == this) {
                        propertySymbol.owner.getter = fn
                    }
                    if (propertySymbol.owner.setter == this) {
                        propertySymbol.owner.setter = this
                    }
                }
            }
            fn.parent = parent
            fn.typeParameters = this.typeParameters.map {
                it.parent = fn
                it
            }
            fn.dispatchReceiverParameter = dispatchReceiverParameter?.copyTo(fn)
            fn.extensionReceiverParameter = extensionReceiverParameter?.copyTo(fn)
            fn.valueParameters = valueParameters.map { p ->
                // Composable lambdas will always have `IrGet`s of all of their parameters
                // generated, since they are passed into the restart lambda. This causes an
                // interesting corner case with "anonymous parameters" of composable functions.
                // If a parameter is anonymous (using the name `_`) in user code, you can usually
                // make the assumption that it is never used, but this is technically not the
                // case in composable lambdas. The synthetic name that kotlin generates for
                // anonymous parameters has an issue where it is not safe to dex, so we sanitize
                // the names here to ensure that dex is always safe.
                p.copyTo(fn, name = dexSafeName(p.name))
            }
            fn.annotations = annotations.map { a -> a }
            fn.metadata = metadata
            fn.body = body
        }
    }

    private fun jvmNameAnnotation(name: String): IrConstructorCall {
        val jvmName = getTopLevelClass(DescriptorUtils.JVM_NAME)
        val constructor = jvmName.constructors.first { it.owner.isPrimary }
        val type = jvmName.createType(false, emptyList())
        return IrConstructorCallImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            type,
            constructor,
            0, 0, 1
        ).also {
            it.putValueArgument(
                0,
                IrConstImpl.string(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    builtIns.stringType,
                    name
                )
            )
        }
    }

    private fun IrFunction.requiresDefaultParameter(): Boolean {
        // we only add a default mask parameter if one of the parameters has a default
        // expression. Note that if this is a "fake override" method, then only the overridden
        // symbols will have the default value expressions
        return this is IrSimpleFunction && (valueParameters.any {
            it.defaultValue != null
        } || overriddenSymbols.any { it.owner.requiresDefaultParameter() })
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun IrFunction.copyWithFragmentParam(): IrSimpleFunction {
        println("COPYING FUNCTION WITH FRAGMENT PARAM " + this.dump())
        assert(explicitParameters.lastOrNull()?.name != NameConventions.CONTEXT_PARAMETER) {
            "Attempted to add fragment param to $this, but it has already been added."
        }

        return copy().also { fn ->
            val oldFn = this

            // NOTE: it's important to add these here before we recurse into the body in
            // order to avoid an infinite loop on circular/recursive calls
            transformedFunctionSet.add(fn)
            transformedFunctions[oldFn as IrSimpleFunction] = fn

            // The overridden symbols might also be composable functions, so we want to make sure
            // and transform them as well
            if (this is IrOverridableDeclaration<*>) {
                fn.overriddenSymbols = overriddenSymbols.map {
                    it as IrSimpleFunctionSymbol
                    val owner = it.owner
                    val newOwner = owner.withFragmentParamIfNeeded()
                    newOwner.symbol as IrSimpleFunctionSymbol
                }
            }

            // if we are transforming a composable property, the jvm signature of the
            // corresponding getters and setters have a composer parameter. Since Kotlin uses the
            // lack of a parameter to determine if it is a getter, this breaks inlining for
            // composable property getters since it ends up looking for the wrong jvmSignature.
            // In this case, we manually add the appropriate "@JvmName" annotation so that the
            // inliner doesn't get confused.
            val descriptor = descriptor
            if (descriptor is PropertyGetterDescriptor &&
                    fn.annotations.findAnnotation(DescriptorUtils.JVM_NAME) == null
            ) {
                val name = JvmAbi.getterName(descriptor.correspondingProperty.name.identifier)
                fn.annotations += jvmNameAnnotation(name)
                fn.correspondingPropertySymbol?.owner?.getter = fn
            }

            // same thing for the setter
            if (descriptor is PropertySetterDescriptor &&
                fn.annotations.findAnnotation(DescriptorUtils.JVM_NAME) == null
            ) {
                val name = JvmAbi.setterName(descriptor.correspondingProperty.name.identifier)
                fn.annotations += jvmNameAnnotation(name)
                fn.correspondingPropertySymbol?.owner?.setter = fn
            }

            fn.valueParameters = fn.valueParameters.map { param ->
                val newType = defaultParameterType(param)
                param.copyTo(fn, type = newType, isAssignable = param.defaultValue != null)
            }

            val valueParametersMapping = explicitParameters
                .zip(fn.explicitParameters)
                .toMap()

            val realParams = fn.valueParameters.size

            // $fragment
            val fragmentParam = fn.addValueParameter {
                name = NameConventions.CONTEXT_PARAMETER
                type = fragmentType.makeNullable()
                origin = IrDeclarationOrigin.DEFINED
                isAssignable = true
            }

            // $changed[n]
            val changed = NameConventions.CHANGED_PARAMETER.identifier
            for (i in 0 until changedParamCount(realParams, fn.thisParamCount)) {
                fn.addValueParameter(
                    if (i == 0) changed else "$changed$i",
                    context.irBuiltIns.intType
                )
            }

            // $default[n]
            if (fn.requiresDefaultParameter()) {
                val defaults = NameConventions.DEFAULT_PARAMETER.identifier
                for (i in 0 until defaultParamCount(realParams)) {
                    fn.addValueParameter(
                        if (i == 0) defaults else "$defaults$i",
                        context.irBuiltIns.intType,
                        IrDeclarationOrigin.MASK_FOR_DEFAULT_FUNCTION
                    )
                }
            }

            println("TRANSFORMING CHILDREN : ${fn.dump()}")
            fn.transformChildrenVoid(object : IrElementTransformerVoid() {
                var isNestedScope = false

                override fun visitGetValue(expression: IrGetValue): IrExpression {
                    val newParam = valueParametersMapping[expression.symbol.owner]
                    return if (newParam != null) {
                        IrGetValueImpl(
                            expression.startOffset,
                            expression.endOffset,
                            expression.type,
                            newParam.symbol,
                            expression.origin
                        )
                    } else expression
                }

                override fun visitReturn(expression: IrReturn): IrExpression {
                    if (expression.returnTargetSymbol == oldFn.symbol) {
                        // update the return statement to point to the new function, or else
                        // it will be interpreted as a non-local return
                        return super.visitReturn(
                            IrReturnImpl(
                                expression.startOffset,
                                expression.endOffset,
                                expression.type,
                                fn.symbol,
                                expression.value
                            )
                        )
                    }
                    return super.visitReturn(expression)
                }

                override fun visitFunction(declaration: IrFunction): IrStatement {
                    val wasNested = isNestedScope
                    try {
                        // we don't want to pass the composer parameter in to composable calls
                        // inside of nested scopes.... *unless* the scope was inlined.
                        isNestedScope = if (declaration.isNonFragmentInlinedLambda()) wasNested else true
                        return super.visitFunction(declaration)
                    } finally {
                        isNestedScope = wasNested
                    }
                }

                override fun visitCall(expression: IrCall): IrExpression {
                    println("VISITING CALL " + expression.dump())
                    val expr = if (!isNestedScope) {
                        expression.withFragmentParamIfNeeded(fragmentParam)
                    } else expression
                    return super.visitCall(expr)
                }
            })
        }
    }

    fun defaultParameterType(param: IrValueParameter): IrType {
        val type = param.type
        if (param.defaultValue == null) return type
        return when {
            type.isPrimitiveType() -> type
            type.isInlined() -> type
            else -> type.makeNullable()
        }
    }

    fun IrCall.isInlineParameterLambdaInvoke(): Boolean {
        if (origin != IrStatementOrigin.INVOKE) return false
        val lambda = dispatchReceiver as? IrGetValue
        val valueParameter = lambda?.symbol?.owner as? IrValueParameter
        return valueParameter?.isInlineParameter() == true
    }

    fun IrCall.isFragmentLambdaInvoke(): Boolean {
        return isInvoke() && dispatchReceiver?.type?.hasFragmentAnnotation() == true
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun IrFunction.isNonFragmentInlinedLambda(): Boolean {
        for (element in inlinedFunctions) {
            if (element.argument.function != this)
                continue
            if (!element.parameter.descriptor.type.hasFragmentAnnotation())
                return true
        }
        return false
    }

    /**
     * With klibs, composable functions are always deserialized from IR instead of being restored
     * into stubs.
     * In this case, we need to avoid transforming those functions twice (because synthetic
     * parameters are being added). We know however, that all the other modules were compiled
     * before, so if the function comes from other [IrModuleFragment], we must skip it.
     *
     * NOTE: [ModuleDescriptor] will not work here, as incremental compilation of the same module
     * can contain some functions that were transformed during previous compilation in a
     * different module fragment with the same [ModuleDescriptor]
     *
     * todo this should be removable since I am ignoring klibs
     */
    private fun IrFunction.externallyTransformed(): Boolean =
        /*decoysEnabled*/false && currentModule?.files?.contains(fileOrNull) != true
}