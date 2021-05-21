package com.mattworzala.canvas.compiler.fragment

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getValueArgumentForExpression
import org.jetbrains.kotlin.resolve.calls.checkers.AdditionalTypeChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.inline.InlineUtil.*
import org.jetbrains.kotlin.resolve.sam.getSingleAbstractMethodOrNull
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.lowerIfFlexible
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlin.types.typeUtil.isAnyOrNullableAny
import org.jetbrains.kotlin.types.upperIfFlexible
import org.jetbrains.kotlin.util.OperatorNameConventions

open class FragmentCallChecker :
    CallChecker,
    AdditionalTypeChecker,
    StorageComponentContainerContributor {
    override fun registerModuleComponents(
        container: StorageComponentContainer,
        platform: TargetPlatform,
        moduleDescriptor: ModuleDescriptor
    ) {
        if (platform.isJvm())
            container.useInstance(this)
    }

    fun checkInlineLambdaCall(
        resolvedCall: ResolvedCall<*>,
        reportOn: PsiElement,
        context: CallCheckerContext
    ) {
        if (resolvedCall !is VariableAsFunctionResolvedCall) return
        val descriptor = resolvedCall.variableCall.resultingDescriptor
        if (descriptor !is ValueParameterDescriptor) return
        // if has disallow fragment annotation return
        val function = descriptor.containingDeclaration
        if (
            function is FunctionDescriptor &&
            function.isInline &&
            function.isMarkedAsFragment()
        ) {
            val bindingContext = context.trace.bindingContext
            var node: PsiElement? = reportOn
            loop@ while (node != null) {
                when (node) {
                    is KtLambdaExpression -> {
//                        val arg = getArgumentDescriptor(node.functionLiteral, bindingContext)
//                        if (arg?.type?.hasDisallowComposableCallsAnnotation() == true) {
//                            val parameterSrc = descriptor.findPsi()
//                            if (parameterSrc != null) {
//                                missingDisallowedComposableCallPropagation(
//                                    context,
//                                    parameterSrc,
//                                    descriptor,
//                                    arg
//                                )
//                            }
                    }
                    is KtFunction -> {
                        val fn = bindingContext[BindingContext.FUNCTION, node]
                        if (fn == function) return
                    }
                }
                node = node.parent as? KtElement
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        if (!resolvedCall.isFragmentInvocation()) {
            checkInlineLambdaCall(resolvedCall, reportOn, context)
            return
        }

        val bindingContext = context.trace.bindingContext
        var node: PsiElement? = reportOn
        loop@ while (node != null) {
            when (node) {
                is KtFunctionLiteral -> {
                    // keep going, as this is a "KtFunction", but we actually want the
                    // KtLambdaExpression
                }
                is KtLambdaExpression -> {
                    val descriptor = bindingContext[BindingContext.FUNCTION, node.functionLiteral]
                    if (descriptor == null) {
                        illegalCall(context, reportOn)
                        return
                    }
                    val fragment = descriptor.isFragmentCallable(bindingContext)
                    if (fragment) return
                    val arg = getArgumentDescriptor(node.functionLiteral, bindingContext)
//                    if (arg?.type?.hasDisallowComposableCallsAnnotation() == true) {
//                        context.trace.record(
//                            ComposeWritableSlices.LAMBDA_CAPABLE_OF_COMPOSER_CAPTURE,
//                            descriptor,
//                            false
//                        )
//                        context.trace.report(
//                            CanvasErrors.CAPTURED_FRAGMENT_INVOCATION.on(
//                                reportOn,
//                                arg,
//                                arg.containingDeclaration
//                            )
//                        )
//                        return
//                    }
                    val argTypeDescriptor = arg
                        ?.type
                        ?.constructor
                        ?.declarationDescriptor as? ClassDescriptor
                    if (argTypeDescriptor != null) {
                        val sam = getSingleAbstractMethodOrNull(argTypeDescriptor)
                        if (sam != null && sam.hasFragmentAnnotation())
                            return
                    }

                    // TODO(lmr): in future, we should check for CALLS_IN_PLACE contract
                    val inlined = arg != null &&
                            canBeInlineArgument(node.functionLiteral) &&
                            isInline(arg.containingDeclaration) &&
                            isInlineParameter(arg)
                    if (!inlined) {
                        illegalCall(context, reportOn)
                        return
                    } else {
                        // since the function is inlined, we continue going up the PSI tree
                        // until we find a composable context. We also mark this lambda
                        context.trace.record(
                            FragmentWritableSlices.LAMBDA_CAPABLE_OF_COMPOSER_CAPTURE,
                            descriptor, true
                        )
                    }
                }
                is KtTryExpression -> {
                    val tryKeyword = node.tryKeyword
                    if (node.tryBlock.textRange.contains(reportOn.textRange) && tryKeyword != null) {
                        context.trace.report(
                            CanvasErrors.ILLEGAL_TRY_CATCH_AROUND_FRAGMENT.on(tryKeyword)
                        )
                    }
                }
                is KtFunction -> {
                    val descriptor = bindingContext[BindingContext.FUNCTION, node]
                    println("CHECKING FUNCTION: " + descriptor.toString())
                    if (descriptor == null) {
                        println("A")
                        illegalCall(context, reportOn)
                        return
                    }
                    val fragment = descriptor.isFragmentCallable(bindingContext)
                    if (!fragment) {
                        println("B")
                        illegalCall(context, reportOn, node.nameIdentifier ?: node)
                    }
//                    if (descriptor.hasReadonlyComposableAnnotation()) {
//                    enforce that the original call was readonly
//                        if (!resolvedCall.isReadOnlyComposableInvocation()) {
//                            illegalCallMustBeReadonly(
//                                context,
//                                reportOn
//                            )
//                        }
//                    }
                    return
                }
                is KtProperty -> {
                    // NOTE: since we're explicitly going down a different branch for
                    // KtPropertyAccessor, the ONLY time we make it into this branch is when the
                    // call was done in the initializer of the property/variable.
                    val descriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, node]
                    if (
                        descriptor !is LocalVariableDescriptor &&
                        node.annotationEntries.hasFragmentAnnotation(bindingContext)
                    ) {
                        // fragments shouldn't have initializers in the first place
                        illegalCall(context, reportOn)
                        return
                    }
                }
                is KtPropertyAccessor -> {
                    val property = node.property
                    val isFragment = node.annotationEntries.hasFragmentAnnotation(bindingContext)
                    if (!isFragment) {
                        illegalCall(context, reportOn, property.nameIdentifier ?: property)
                    }
                    val descriptor = bindingContext[BindingContext.PROPERTY_ACCESSOR, node] ?: return
//                    if (descriptor.hasReadonlyComposableAnnotation()) {
//                        // enforce that the original call was readonly
//                        if (!resolvedCall.isReadOnlyComposableInvocation()) {
//                            illegalCallMustBeReadonly(
//                                context,
//                                reportOn
//                            )
//                        }
//                    }
                    return
                }
                is KtCallableReferenceExpression -> {
                    illegalFragmentFunctionReference(context, node)
                    return
                }
                is KtFile -> {
                    // if we've made it this far, the call was made in a non-fragment context.
                    illegalCall(context, reportOn)
                    return
                }
                is KtClass -> {
                    // fragment calls are never allowed in the initializers of a class
                    illegalCall(context, reportOn)
                    return
                }
            }
            node = node.parent as? KtElement
        }
    }

    private fun missingDisallowedComposableCallPropagation(
        context: CallCheckerContext,
        unmarkedParamEl: PsiElement,
        unmarkedParamDescriptor: ValueParameterDescriptor,
        markedParamDescriptor: ValueParameterDescriptor
    ) {
        context.trace.report(
            CanvasErrors.MISSING_DISALLOW_FRAGMENT_CALLS_ANNOTATION.on(
                unmarkedParamEl,
                unmarkedParamDescriptor,
                markedParamDescriptor,
                markedParamDescriptor.containingDeclaration
            )
        )
    }

    private fun illegalCall(
        context: CallCheckerContext,
        callEl: PsiElement,
        functionEl: PsiElement? = null
    ) {
        context.trace.report(CanvasErrors.FRAGMENT_INVOCATION.on(callEl))
        if (functionEl != null) {
            context.trace.report(CanvasErrors.FRAGMENT_EXPECTED.on(functionEl))
        }
    }

    private fun illegalCallMustBeReadonly(
        context: CallCheckerContext,
        callEl: PsiElement
    ) {
        context.trace.report(CanvasErrors.NONREADONLY_CALL_IN_READONLY_FRAGMENT.on(callEl))
    }

    private fun illegalFragmentFunctionReference(
        context: CallCheckerContext,
        refExpr: KtCallableReferenceExpression
    ) {
        context.trace.report(CanvasErrors.FRAGMENT_FUNCTION_REFERENCE.on(refExpr))
    }

    override fun checkType(
        expression: KtExpression,
        expressionType: KotlinType,
        expressionTypeWithSmartCast: KotlinType,
        c: ResolutionContext<*>
    ) {
        val bindingContext = c.trace.bindingContext
        val expectedType = c.expectedType
        if (expectedType === TypeUtils.NO_EXPECTED_TYPE) return
        if (expectedType === TypeUtils.UNIT_EXPECTED_TYPE) return
        if (expectedType.isAnyOrNullableAny()) return
        val expectedFragment = expectedType.hasFragmentAnnotation()
        if (expression is KtLambdaExpression) {
            val descriptor = bindingContext[BindingContext.FUNCTION, expression.functionLiteral] ?: return
            val isFragment = descriptor.isFragmentCallable(bindingContext)
            if (expectedFragment != isFragment) {
                val isInlineable = isInlinedArgument(
                    expression.functionLiteral,
                    c.trace.bindingContext,
                    true
                )
                if (isInlineable) return

                if (!expectedFragment && isFragment) {
                    val inferred = c.trace.bindingContext[
                            FragmentWritableSlices.INFERRED_COMPOSABLE_DESCRIPTOR,
                            descriptor
                    ] == true
                    if (inferred) return
                }

                val reportOn =
                    if (expression.parent is KtAnnotatedExpression)
                        expression.parent as KtExpression
                    else expression
                c.trace.report(
                    CanvasErrors.TYPE_MISMATCH.on(
                        reportOn,
                        expectedType,
                        expressionTypeWithSmartCast
                    )
                )
            }
            return
        } else {
            val nullableAnyType = expectedType.builtIns.nullableAnyType
            val anyType = expectedType.builtIns.anyType

            if (anyType == expectedType.lowerIfFlexible() &&
                nullableAnyType == expectedType.upperIfFlexible()
            ) return

            val nullableNothingType = expectedType.builtIns.nullableNothingType

            // Handle assigning null to a nullable composable type
            if (expectedType.isMarkedNullable &&
                expressionTypeWithSmartCast == nullableNothingType
            ) return
            val isFragment = expressionType.hasFragmentAnnotation()

            if (expectedFragment != isFragment) {
                val reportOn =
                    if (expression.parent is KtAnnotatedExpression)
                        expression.parent as KtExpression
                    else expression
                c.trace.report(
                    CanvasErrors.TYPE_MISMATCH.on(
                        reportOn,
                        expectedType,
                        expressionTypeWithSmartCast
                    )
                )
            }
        }
    }
}

//todo read only, unused for now
//fun ResolvedCall<*>.isReadOnlyFragmentInvocation(): Boolean {
//    if (this is VariableAsFunctionResolvedCall) {
//        return false
//    }
//    val candidateDescriptor = candidateDescriptor
//    return when (candidateDescriptor) {
//        is ValueParameterDescriptor -> false
//        is LocalVariableDescriptor -> false
//        is PropertyDescriptor -> {
//            val isGetter = valueArguments.isEmpty()
//            val getter = candidateDescriptor.getter
//            if (isGetter && getter != null) {
//                getter.hasReadonlyFragmentAnnotation()
//            } else {
//                false
//            }
//        }
//        is PropertyGetterDescriptor -> candidateDescriptor.hasReadonlyFragmentAnnotation()
//        else -> candidateDescriptor.hasReadonlyFragmentAnnotation()
//    }
//}

fun ResolvedCall<*>.isFragmentInvocation(): Boolean {
    if (this is VariableAsFunctionResolvedCall) {
        if (variableCall.candidateDescriptor.type.hasFragmentAnnotation())
            return true
        if (functionCall.resultingDescriptor.hasFragmentAnnotation()) return true
        return false
    }
    val candidateDescriptor = candidateDescriptor
    if (candidateDescriptor is FunctionDescriptor) {
        if (candidateDescriptor.isOperator &&
            candidateDescriptor.name == OperatorNameConventions.INVOKE
        ) {
            if (dispatchReceiver?.type?.hasFragmentAnnotation() == true) {
                return true
            }
        }
    }
    return when (candidateDescriptor) {
        is ValueParameterDescriptor -> false
        is LocalVariableDescriptor -> false
        is PropertyDescriptor -> {
            val isGetter = valueArguments.isEmpty()
            val getter = candidateDescriptor.getter
            if (isGetter && getter != null) {
                getter.hasFragmentAnnotation()
            } else {
                false
            }
        }
        is PropertyGetterDescriptor -> candidateDescriptor.hasFragmentAnnotation()
        else -> candidateDescriptor.hasFragmentAnnotation()
    }
}

internal fun CallableDescriptor.isMarkedAsFragment(): Boolean {
    return when (this) {
        is PropertyGetterDescriptor -> hasFragmentAnnotation()
        is ValueParameterDescriptor -> type.hasFragmentAnnotation()
        is LocalVariableDescriptor -> type.hasFragmentAnnotation()
        is PropertyDescriptor -> false
        else -> hasFragmentAnnotation()
    }
}

// if you called this, it would need to be a fragment call (fragment, changed, etc.)
fun CallableDescriptor.isFragmentCallable(bindingContext: BindingContext): Boolean {
    println("B.0")
    // if it's marked as fragment then we're done
    if (isMarkedAsFragment()) return true
    println("B.01")
    if (
        this is FunctionDescriptor &&
        bindingContext[FragmentWritableSlices.INFERRED_COMPOSABLE_DESCRIPTOR, this] == true
    ) {
        // even though it's not marked, it is inferred as so by the type system (by being passed
        // into a parameter marked as fragment or a variable typed as one. This isn't much
        // different than being marked explicitly.
        return true
    }
    println("B.02")
    val functionLiteral = findPsi() as? KtFunctionLiteral
    // if it isn't a function literal then we are out of things to try.
        ?: return false
    println("B.1")

    if (functionLiteral.annotationEntries.hasFragmentAnnotation(bindingContext)) {
        // in this case the function literal itself is being annotated as fragment but the
        // annotation isn't in the descriptor itself
        return true
    }
    val lambdaExpr = functionLiteral.parent as? KtLambdaExpression
    if (
        lambdaExpr != null &&
        bindingContext[FragmentWritableSlices.INFERRED_COMPOSABLE_LITERAL, lambdaExpr] == true
    ) {
        // this lambda was marked as inferred to be fragment
        return true
    }
    // TODO(lmr): i'm not sure that this is actually needed at this point, since this should have
    //  been covered by the TypeResolutionInterceptorExtension
    val arg = getArgumentDescriptor(functionLiteral, bindingContext) ?: return false
    println("B.2 + ${arg.type.hasFragmentAnnotation()}")
    return arg.type.hasFragmentAnnotation()
}

// the body of this function can have fragment calls in it, even if it itself is not
// fragment (it might capture a fragment from the parent)
fun FunctionDescriptor.allowsFragmentCalls(bindingContext: BindingContext): Boolean {
    // if it's callable as a fragment, then the answer is yes.
    if (isFragmentCallable(bindingContext)) return true
    // otherwise, this is only true if it is a lambda which can be capable of fragment
    // capture
    return bindingContext[
            FragmentWritableSlices.LAMBDA_CAPABLE_OF_COMPOSER_CAPTURE,
            this
    ] == true
}

internal fun getArgumentDescriptor(
    argument: KtFunction,
    bindingContext: BindingContext
): ValueParameterDescriptor? {
    val call = KtPsiUtil.getParentCallIfPresent(argument) ?: return null
    val resolvedCall = call.getResolvedCall(bindingContext) ?: return null
    val valueArgument = resolvedCall.call.getValueArgumentForExpression(argument) ?: return null
    val mapping = resolvedCall.getArgumentMapping(valueArgument) as? ArgumentMatch ?: return null
    return mapping.valueParameter
}

fun List<KtAnnotationEntry>.hasFragmentAnnotation(bindingContext: BindingContext): Boolean {
    for (entry in this) {
        val descriptor = bindingContext.get(BindingContext.ANNOTATION, entry) ?: continue
        if (descriptor.isFragmentAnnotation) return true
    }
    return false
}