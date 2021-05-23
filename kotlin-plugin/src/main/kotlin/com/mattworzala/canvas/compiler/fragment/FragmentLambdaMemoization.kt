package com.mattworzala.canvas.compiler.fragment

import com.mattworzala.canvas.compiler.lower.ModuleLoweringPass
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContextImpl
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.copyAnnotations
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createParameterDeclarations
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.peek
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.addAnnotations
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.psi.addRemoveModifier.addAnnotationEntry
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.typeUtil.isUnit
import kotlin.math.exp

private class CaptureCollector {
    val captures = mutableSetOf<IrValueDeclaration>()
    val capturedFunctions = mutableSetOf<IrFunction>()
    val hasCaptures: Boolean get() = captures.isNotEmpty() || capturedFunctions.isNotEmpty()

    fun recordCapture(local: IrValueDeclaration) {
        captures.add(local)
    }

    fun recordCapture(local: IrFunction) {
        capturedFunctions.add(local)
    }
}

private abstract class DeclarationContext {
    abstract val fragment: Boolean
    abstract val symbol: IrSymbol
    abstract val functionContext: FunctionContext?
    abstract fun declareLocal(local: IrValueDeclaration?)
    abstract fun recordLocalFunction(local: FunctionContext)
    abstract fun recordCapture(local: IrValueDeclaration?)
    abstract fun recordCapture(local: IrFunction?)
    abstract fun pushCollector(collector: CaptureCollector)
    abstract fun popCollector(collector: CaptureCollector)
}

private class SymbolOwnerContext(val declaration: IrSymbolOwner): DeclarationContext() {
    override val fragment get() = false
    override val functionContext: FunctionContext? get() = null
    override val symbol get() = declaration.symbol
    override fun declareLocal(local: IrValueDeclaration?) { }
    override fun recordLocalFunction(local: FunctionContext) { }
    override fun recordCapture(local: IrValueDeclaration?) { }
    override fun recordCapture(local: IrFunction?) { }
    override fun pushCollector(collector: CaptureCollector) { }
    override fun popCollector(collector: CaptureCollector) { }
}

private class FunctionLocalSymbol(
    val declaration: IrSymbolOwner,
    override val functionContext: FunctionContext
) : DeclarationContext() {
    override val fragment: Boolean get() = functionContext.fragment
    override val symbol: IrSymbol get() = declaration.symbol
    override fun declareLocal(local: IrValueDeclaration?) = functionContext.declareLocal(local)
    override fun recordLocalFunction(local: FunctionContext) = functionContext.recordLocalFunction(local)
    override fun recordCapture(local: IrValueDeclaration?) = functionContext.recordCapture(local)
    override fun recordCapture(local: IrFunction?) = functionContext.recordCapture(local)
    override fun pushCollector(collector: CaptureCollector) = functionContext.pushCollector(collector)
    override fun popCollector(collector: CaptureCollector) = functionContext.popCollector(collector)
}

private class FunctionContext(
    val declaration: IrFunction,
    override val fragment: Boolean,
    val canRemember: Boolean
) : DeclarationContext() {
    override val symbol get() = declaration.symbol
    override val functionContext: FunctionContext get() = this
    val locals = mutableSetOf<IrValueDeclaration>()
    val captures = mutableSetOf<IrValueDeclaration>()
    var collectors = mutableListOf<CaptureCollector>()
    val localFunctionCaptures = mutableMapOf<IrFunction, Set<IrValueDeclaration>>()

    init {
        declaration.valueParameters.forEach {
            declareLocal(it)
        }
        declaration.dispatchReceiverParameter?.let { declareLocal(it) }
        declaration.extensionReceiverParameter?.let { declareLocal(it) }
    }

    override fun declareLocal(local: IrValueDeclaration?) {
        if (local != null) {
            locals.add(local)
        }
    }

    override fun recordLocalFunction(local: FunctionContext) {
        if (local.captures.isNotEmpty() && local.declaration.isLocal) {
            localFunctionCaptures[local.declaration] = local.captures
        }
    }

    override fun recordCapture(local: IrValueDeclaration?) {
        if (local != null && collectors.isNotEmpty() && locals.contains(local)) {
            for (collector in collectors) {
                collector.recordCapture(local)
            }
        }
        if (local != null && declaration.isLocal && !locals.contains(local)) {
            captures.add(local)
        }
    }

    override fun recordCapture(local: IrFunction?) {
        if (local != null) {
            val captures = localFunctionCaptures[local]
            for (collector in collectors) {
                collector.recordCapture(local)
                if (captures != null) {
                    for (capture in captures) {
                        collector.recordCapture(capture)
                    }
                }
            }
        }
    }

    override fun pushCollector(collector: CaptureCollector) {
        collectors.add(collector)
    }

    override fun popCollector(collector: CaptureCollector) {
        require(collectors.lastOrNull() == collector)
        collectors.removeAt(collectors.size - 1)
    }
}

private class ClassContext(val declaration: IrClass) : DeclarationContext() {
    override val fragment: Boolean = false
    override val symbol get() = declaration.symbol
    override val functionContext: FunctionContext? = null
    val thisParam: IrValueDeclaration? = declaration.thisReceiver!!
    var collectors = mutableListOf<CaptureCollector>()
    override fun declareLocal(local: IrValueDeclaration?) { }
    override fun recordLocalFunction(local: FunctionContext) { }
    override fun recordCapture(local: IrValueDeclaration?) {
        if (local != null && collectors.isNotEmpty() && local == thisParam) {
            for (collector in collectors) {
                collector.recordCapture(local)
            }
        }
    }
    override fun recordCapture(local: IrFunction?) { }
    override fun pushCollector(collector: CaptureCollector) {
        collectors.add(collector)
    }
    override fun popCollector(collector: CaptureCollector) {
        require(collectors.lastOrNull() == collector)
        collectors.removeAt(collectors.size - 1)
    }
}

const val FRAGMENT_LAMBDA = "fragmentLambda"
const val FRAGMENT_LAMBDA_N = "fragmentLambdaN"
const val FRAGMENT_LAMBDA_INSTANCE = "fragmentLambdaInstance"
const val FRAGMENT_LAMBDA_N_INSTANCE = "fragmentLambdaNInstance"

class FragmentLambdaMemoization(
    context: IrPluginContext,
    symbolRemapper: DeepCopySymbolRemapper,
    bindingTrace: BindingTrace
) : AbstractFragmentLowering(context, symbolRemapper, bindingTrace), ModuleLoweringPass {
    private val declarationContextStack = mutableListOf<DeclarationContext>()

    private val currentFunctionContext: FunctionContext? get() =
        declarationContextStack.peek()?.functionContext

    private var fragmentSingletonsClass: IrClass? = null
    private var currentFile: IrFile? = null

    private fun getOrCreateFragmentSingletonsClass(): IrClass {
        if (fragmentSingletonsClass != null) return fragmentSingletonsClass!!
        val declaration = currentFile!!
        val filePath = declaration.fileEntry.name
        val fileName = filePath.split('/').last()
        val current = context.irFactory.buildClass {
            startOffset = SYNTHETIC_OFFSET
            endOffset = SYNTHETIC_OFFSET
            kind = ClassKind.OBJECT
            visibility = DescriptorVisibilities.INTERNAL
            val shortName = PackagePartClassUtils.getFilePartShortName(fileName)
            //todo not using live literals atm
            // the name of the LiveLiterals class is per-file, so we use the same name that
            // the kotlin file class lowering produces, prefixed with `LiveLiterals$`.
            name = Name.identifier("FragmentSingletons${"$"}$shortName")
        }.also {
            it.createParameterDeclarations()

            // store the full file path to the file that this class is associated with in an
            // annotation on the class. This will be used by tooling to associate the keys
            // inside of this class with actual PSI in the editor.
            it.addConstructor {
                isPrimary = true
            }.also { constructor ->
                constructor.body = DeclarationIrBuilder(context, it.symbol).irBlockBody {
                    +irDelegatingConstructorCall(
                        context
                            .irBuiltIns
                            .anyClass
                            .owner
                            .primaryConstructor!!
                    )
                    +IrInstanceInitializerCallImpl(
                        startOffset = this.startOffset,
                        endOffset = this.endOffset,
                        classSymbol = it.symbol,
                        type = it.defaultType
                    )
                }
            }
        }.markAsFragmentSingletonClass()
        fragmentSingletonsClass = current
        return current
    }

    override fun visitFile(declaration: IrFile): IrFile {
        val prevFile = currentFile
        val prevClass = fragmentSingletonsClass
        try {
            currentFile = declaration
            fragmentSingletonsClass = null
            val file = super.visitFile(declaration)
            // if there were no constants found in the entire file, then we don't need to
            // create this class at all
            val resultingClass = fragmentSingletonsClass
            if (resultingClass != null && resultingClass.declarations.isNotEmpty()) {
                file.addChild(resultingClass)
            }
            return file
        } finally {
            currentFile = prevFile
            fragmentSingletonsClass = prevClass
        }
    }

    override fun lower(module: IrModuleFragment) {
        super.lower(module)
        module.transformChildrenVoid(this)
    }

    override fun visitDeclaration(declaration: IrDeclarationBase): IrStatement {
        if (declaration is IrFunction)
            return super.visitDeclaration(declaration)
        val symbolOwner = declaration as? IrSymbolOwner
        if (symbolOwner != null) {
            val functionContext = currentFunctionContext
            if (functionContext != null)
                declarationContextStack.push(FunctionLocalSymbol(declaration, functionContext))
            else declarationContextStack.push(SymbolOwnerContext(declaration))
        }
        val result = super.visitDeclaration(declaration)
        if (symbolOwner != null) declarationContextStack.pop()
        return result
    }

    private fun irCurrentFragment(): IrExpression {
        val currentFragmentSymbol = getTopLevelPropertyGetter(
            FragmentFqNames.fqNameFor("currentFragment")
        )

        return IrCallImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            fragmentIrClass.defaultType.replaceArgumentsWithStarProjections(),
            currentFragmentSymbol as IrSimpleFunctionSymbol,
            currentFragmentSymbol.owner.typeParameters.size,
            currentFragmentSymbol.owner.valueParameters.size,
            IrStatementOrigin.FOR_LOOP_ITERATOR,
        )
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun visitFunction(declaration: IrFunction): IrStatement {
        val descriptor = declaration.descriptor
        val fragment = descriptor.allowsFragmentCalls()
        val canRemember = fragment &&
                // Don't use remember in an inline function
                !descriptor.isInline &&
                // Don't use remember if in a composable that returns a value
                descriptor.returnType.let { it != null && it.isUnit() }
        val context = FunctionContext(declaration, fragment, canRemember)
        declarationContextStack.push(context)
        val result = super.visitFunction(declaration)
        declarationContextStack.pop()
        if (declaration.isLocal) {
            declarationContextStack.peek()?.recordLocalFunction(context)
        }
        return result
    }

    override fun visitClass(declaration: IrClass): IrStatement {
        val context = ClassContext(declaration)
        declarationContextStack.push(context)
        val result = super.visitClass(declaration)
        declarationContextStack.pop()
        return result
    }

    override fun visitVariable(declaration: IrVariable): IrStatement {
        declarationContextStack.peek()?.declareLocal(declaration)
        return super.visitVariable(declaration)
    }

    override fun visitValueAccess(expression: IrValueAccessExpression): IrExpression {
        declarationContextStack.forEach {
            it.recordCapture(expression.symbol.owner)
        }
        return super.visitValueAccess(expression)
    }

    override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
        // Memoize the instance created by using the :: operator
        val result = super.visitFunctionReference(expression)
        val functionContext = currentFunctionContext ?: return result
        if (expression.valueArgumentsCount != 0) {
            // If this syntax is as a curry syntax in the future, don't memoize.
            // The syntax <expr>::<method>(<params>) and ::<function>(<params>) is reserved for
            // future use. This ensures we don't try to memoize this syntax without knowing
            // its meaning.

            // The most likely correct implementation is to treat the parameters exactly as the
            // receivers are treated below.
            return result
        }
        if (functionContext.canRemember) {
            // Memoize the reference for <expr>::<method>
            val dispatchReceiver = expression.dispatchReceiver
            val extensionReceiver = expression.extensionReceiver
            if ((dispatchReceiver != null || extensionReceiver != null)
                && dispatchReceiver.isNullOrStable()
                && extensionReceiver.isNullOrStable()) {
                // Save the receivers into a temporaries and memoize the function reference using
                // the resulting temporaries
                val builder = DeclarationIrBuilder(
                    generatorContext = context,
                    symbol = functionContext.symbol,
                    startOffset = expression.startOffset,
                    endOffset = expression.endOffset
                )
                return builder.irBlock(
                    resultType = expression.type
                ) {
                    val captures = mutableListOf<IrValueDeclaration>()

                    val tempDispatchReceiver = dispatchReceiver?.let {
                        val tmp = irTemporary(it)
                        captures.add(tmp)
                        tmp
                    }
                    val tempExtensionReceiver = extensionReceiver?.let {
                        val tmp = irTemporary(it)
                        captures.add(tmp)
                        tmp
                    }

                    +rememberExpression(
                        functionContext,
                        IrFunctionReferenceImpl(
                            startOffset,
                            endOffset,
                            expression.type,
                            expression.symbol,
                            expression.typeArgumentsCount,
                            expression.valueArgumentsCount,
                            expression.reflectionTarget
                        ).copyAttributes(expression).apply {
                            this.dispatchReceiver = tempDispatchReceiver?.let { irGet(it) }
                            this.extensionReceiver = tempExtensionReceiver?.let { irGet(it) }
                        },
                        captures
                    )
                }
            } else if (dispatchReceiver == null) {
                return rememberExpression(functionContext, result, emptyList())
            }
        }
        return result
    }

    private fun visitNonFragmentFunctionExpression(
        expression: IrFunctionExpression
    ): IrExpression {
        val functionContext = currentFunctionContext
            ?: return super.visitFunctionExpression(expression)

        if (
            // Only memoize non-composable lambdas in a context we can use remember
            !functionContext.canRemember ||
            // Don't memoize inlined lambdas
            expression.function.isInlinedLambda()
        ) {
            return super.visitFunctionExpression(expression)
        }

        // Record capture variables for this scope
        val collector = CaptureCollector()
        startCollector(collector)
        // Wrap fragment functions expressions or memoize non-fragment function expressions
        val result = super.visitFunctionExpression(expression)
        stopCollector(collector)

        // If the ancestor converted this then return
        val functionExpression = result as? IrFunctionExpression ?: return result

        return rememberExpression(
            functionContext,
            functionExpression,
            collector.captures.toList()
        )
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val fn = expression.symbol.owner
        if (fn.isLocal) {
            declarationContextStack.forEach {
                it.recordCapture(fn)
            }
        }
        return super.visitCall(expression)
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun visitFragmentFunctionExpression(
        expression: IrFunctionExpression,
        declarationContext: DeclarationContext
    ): IrExpression {
        val collector = CaptureCollector()
        startCollector(collector)
        val result = super.visitFunctionExpression(expression)
        stopCollector(collector)

        // If the ancestor converted this then return
        val functionExpression = result as? IrFunctionExpression ?: return result

        // Do not wrap target of an inline function
        if (expression.function.isInlinedLambda()) {
            return functionExpression
        }

        // Do not wrap fragment lambdas with return results
        if (!functionExpression.function.descriptor.returnType.let { it == null || it.isUnit() }) {
            return functionExpression
        }

        val wrapped = wrapFunctionExpression(declarationContext, functionExpression, collector)

        if (!collector.hasCaptures) {
            println("CREATING LAMBDA FIELD WITH TYPE ${expression.type} ${expression.type.dumpKotlinLike()} ")
            val realType = expression.type.addAnnotations(expression.function.copyAnnotations())
            println(expression.type.annotations)
            println(realType.annotations)
            println(expression.function.annotations)
            println(expression.dump())
            println(wrapped.dump())
            return irGetFragmentSingleton(
                lambdaExpression = wrapped,
                lambdaType = realType
            )
        } else {
            return wrapped
        }
    }

    private fun irGetFragmentSingleton(
        lambdaExpression: IrExpression,
        lambdaType: IrType
    ): IrExpression {
        println("FIELD WITH TYPE ${lambdaType.dumpKotlinLike()}")
        val clazz = getOrCreateFragmentSingletonsClass()
        val lambdaName = "lambda-${clazz.declarations.size}"
        val lambdaProp = clazz.addProperty {
            name = Name.identifier(lambdaName)
            visibility = DescriptorVisibilities.INTERNAL
        }.also { p ->
            p.backingField = context.irFactory.buildField {
                startOffset = SYNTHETIC_OFFSET
                endOffset = SYNTHETIC_OFFSET
                name = Name.identifier(lambdaName)
                type = lambdaType
                visibility = DescriptorVisibilities.INTERNAL
                isStatic = context.platform.isJvm()
            }.also { f ->
                f.correspondingPropertySymbol = p.symbol
                f.parent = clazz
                f.initializer = DeclarationIrBuilder(context, clazz.symbol)
                    .irExprBody(lambdaExpression)
            }
            p.addGetter {
                returnType = lambdaType
                visibility = DescriptorVisibilities.INTERNAL
                origin = IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
            }.also { fn ->
                val thisParam = clazz.thisReceiver!!.copyTo(fn)
                fn.parent = clazz
                fn.dispatchReceiverParameter = thisParam
                fn.body = DeclarationIrBuilder(context, fn.symbol).irBlockBody {
                    +irReturn(irGetField(irGet(thisParam), p.backingField!!))
                }
            }
        }
        return irCall(
            lambdaProp.getter!!.symbol,
            dispatchReceiver = IrGetObjectValueImpl(
                startOffset = UNDEFINED_OFFSET,
                endOffset = UNDEFINED_OFFSET,
                type = clazz.defaultType,
                symbol = clazz.symbol
            )
        ).markAsFragmentSingleton()
    }

    override fun visitFunctionExpression(expression: IrFunctionExpression): IrExpression {
        val declarationContext = declarationContextStack.peek()
            ?: return super.visitFunctionExpression(expression)
        return if (expression.allowsFragmentCalls())
            visitFragmentFunctionExpression(expression, declarationContext)
        else visitNonFragmentFunctionExpression(expression)
    }

    private fun startCollector(collector: CaptureCollector) {
        for (declarationContext in declarationContextStack) {
            declarationContext.pushCollector(collector)
        }
    }

    private fun stopCollector(collector: CaptureCollector) {
        for (declarationContext in declarationContextStack) {
            declarationContext.popCollector(collector)
        }
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun wrapFunctionExpression(
        declarationContext: DeclarationContext,
        expression: IrFunctionExpression,
        collector: CaptureCollector
    ): IrExpression {
        val function = expression.function
        val argumentCount = function.valueParameters.size

        val useFragmentLambdaN = argumentCount > MAX_RESTART_ARGUMENT_COUNT
        val useFragmentFactory = collector.hasCaptures && declarationContext.fragment
        val restartFunctionFactory =
            if (useFragmentFactory)
                if (useFragmentLambdaN)
                    FRAGMENT_LAMBDA_N
                else FRAGMENT_LAMBDA
            else if (useFragmentLambdaN)
                FRAGMENT_LAMBDA_N_INSTANCE
            else FRAGMENT_LAMBDA_INSTANCE
        val restartFactorySymbol =
            getTopLevelFunction(FragmentFqNames.internalFqNameFor(restartFunctionFactory))
        val irBuilder = DeclarationIrBuilder(
            context,
            symbol = declarationContext.symbol,
            startOffset = expression.startOffset,
            endOffset = expression.endOffset
        )

        (context as IrPluginContextImpl).linker.getDeclaration(restartFactorySymbol)
        val fragmentLambdaExpression = irBuilder.irCall(restartFactorySymbol).apply {
            var index = 0

            // first parameter is the composer parameter if we are using the composable factory
            if (useFragmentFactory) {
                putValueArgument(
                    index++,
                    irCurrentFragment()
                )
            }

            // key parameter
            putValueArgument(
                index++,
                irBuilder.irInt(
                    symbol.descriptor.fqNameSafe.hashCode() xor expression.startOffset
                )
            )

            // tracked parameter
            // If the lambda has no captures, then kotlin will turn it into a singleton instance,
            // which means that it will never change, thus does not need to be tracked.
            val shouldBeTracked = collector.captures.isNotEmpty()
            putValueArgument(index++, irBuilder.irBoolean(shouldBeTracked))

            // FragmentLambdaN requires the arity
            if (useFragmentLambdaN) {
                // arity parameter
                putValueArgument(index++, irBuilder.irInt(argumentCount))
            }

            // Source information
            putValueArgument(index++, irBuilder.irNull()) //todo

            if (index >= valueArgumentsCount) {
                error(
                    "function = ${
                        function.name.asString()
                    }, count = $valueArgumentsCount, index = $index"
                )
            }

            putValueArgument(index, expression)
        }

        return fragmentLambdaExpression
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun rememberExpression(
        functionContext: FunctionContext,
        expression: IrExpression,
        captures: List<IrValueDeclaration>
    ): IrExpression {
        // If the function doesn't capture, Kotlin's default optimization is sufficient
        if (captures.isEmpty()) return expression

        // If the function captures any unstable values or var declarations, do not memoize
        if (captures.any {
            !((it as? IrVariable)?.isVar != true && false) //stabilityOf(it.type).knownStable())
        }) return expression

        // Otherwise memoize the expression based on the stable captured values
        val rememberParameterCount = captures.size + 1 // One additional parameter for the lambda
        val declaration = functionContext.declaration
        val rememberFunctions = getTopLevelFunctions(
            FragmentFqNames.fqNameFor("remember")
        ).map { it.owner }

        val directRememberFunction = // Exclude the varargs version
            rememberFunctions.singleOrNull {
                it.valueParameters.size == rememberParameterCount &&
                        // Exclude the varargs version
                        it.valueParameters.firstOrNull()?.varargElementType == null
            }
        val rememberFunction = directRememberFunction ?: rememberFunctions.single {
            // use the varargs version
            it.valueParameters.firstOrNull()?.varargElementType != null
        }

        val rememberFunctionSymbol = referenceSimpleFunction(rememberFunction.symbol)

        val irBuilder = DeclarationIrBuilder(
            generatorContext = context,
            symbol = functionContext.symbol,
            startOffset = expression.startOffset,
            endOffset = expression.endOffset
        )

        return irBuilder.irCall(
            callee = rememberFunctionSymbol,
            type = expression.type
        ).apply {
            // The result type type parameter is first, followed by the argument types
            putTypeArgument(0, expression.type)
            val lambdaArgumentIndex = if (directRememberFunction != null) {
                // condition arguments are the first `arg.size` arguments
                for (i in captures.indices) {
                    putValueArgument(1, irBuilder.irGet(captures[i]))
                }
                // The lambda is the last parameter
                captures.size
            } else {
                val parameterType = rememberFunction.valueParameters[0].type
                // Call to the vararg version
                putValueArgument(
                    0,
                    IrVarargImpl(
                        startOffset = UNDEFINED_OFFSET,
                        endOffset = UNDEFINED_OFFSET,
                        type = parameterType,
                        varargElementType = context.irBuiltIns.anyType,
                        elements = captures.map {
                            irBuilder.irGet(it)
                        }
                    )
                )
                1
            }

            val substitutedLambdaType = rememberFunction.valueParameters.last().type.substitute(
                rememberFunction.typeParameters,
                (0 until typeArgumentsCount).map {
                    getTypeArgument(it) as IrType
                }
            )
            putValueArgument(
                lambdaArgumentIndex,
                irBuilder.irLambdaExpression(
                    descriptor = irBuilder.createFunctionDescriptor(
                        substitutedLambdaType
                    ),
                    type = substitutedLambdaType,
                    body = {
                        +irReturn(expression)
                    }
                )
            )
        }.patchDeclarationParents(declaration).markAsSynthetic(mark = true)
    }

    private fun <T : IrFunctionAccessExpression> T.markAsSynthetic(mark: Boolean): T {
        if (mark) {
            // Mark it so the FragmentCallTransformer will insert the correct code around this call
            context.irTrace.record(
                FragmentWritableSlices.IS_SYNTHETIC_FRAGMENT_CALL,
                this, true
            )
        }
        return this
    }

    private fun <T : IrAttributeContainer> T.markAsFragmentSingleton(): T {
        // Mark it so the FragmentCallTransformer can insert source info around this call
        context.irTrace.record(
            FragmentWritableSlices.IS_FRAGMENT_SINGLETON,
            this, true
        )
        return this
    }

    private fun <T : IrAttributeContainer> T.markAsFragmentSingletonClass(): T {
        // Mark it so FragmentCallTransformer can insert source info around this call
        context.irTrace.record(
            FragmentWritableSlices.IS_FRAGMENT_SINGLETON_CLASS,
            this, true
        )
        return this
    }

    private fun IrExpression?.isNullOrStable() = this == null || false //stabilityOf(this).knownStable()
}

// This must match the highest value of FunctionXX which is current Function22
private const val MAX_RESTART_ARGUMENT_COUNT = 22
