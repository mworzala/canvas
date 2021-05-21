/*
 * Copyright 2020 The Android Open Source Project
 * Copyright 2021 Canvas Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mattworzala.canvas.compiler.v2

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory2
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory3
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.PositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.types.KotlinType

object CanvasErrors {

    // error goes on the fragment call in a non-fragment function
    @JvmField
    val FRAGMENT_INVOCATION =
        DiagnosticFactory0.create<PsiElement>(
            Severity.ERROR
        )

    // error goes on the non-fragment function with fragment calls
    @JvmField
    val FRAGMENT_EXPECTED =
        DiagnosticFactory0.create<PsiElement>(
            Severity.ERROR
        )

    // error goes on ...todo
    @JvmField
    val FRAGMENT_FUNCTION_REFERENCE =
        DiagnosticFactory0.create<KtCallableReferenceExpression>(
            Severity.ERROR
        )

    // error goes on ...todo
    @JvmField
    val FRAGMENT_PROPERTY_BACKING_FIELD =
        DiagnosticFactory0.create<PsiElement>(
            Severity.ERROR
        )

    // error goes on ...todo
    @JvmField
    val FRAGMENT_VAR =
        DiagnosticFactory0.create<PsiElement>(
            Severity.ERROR
        )

    // error goes on ...todo
    @JvmField
    val FRAGMENT_SUSPEND_FUN =
        DiagnosticFactory0.create<PsiElement>(
            Severity.ERROR
        )

    // error goes on fragment main functions
    @JvmField
    val FRAGMENT_FUN_MAIN =
        DiagnosticFactory0.create<PsiElement>(
            Severity.ERROR
        )

    // error goes on ...todo
    @JvmField
    val CAPTURED_FRAGMENT_INVOCATION =
        DiagnosticFactory2.create<PsiElement, DeclarationDescriptor, DeclarationDescriptor>(
            Severity.ERROR
        )

    // error goes on ...todo
    @JvmField
    val MISSING_DISALLOW_FRAGMENT_CALLS_ANNOTATION =
        DiagnosticFactory3.create<
            PsiElement,
            ValueParameterDescriptor, // unmarked
            ValueParameterDescriptor, // marked
            CallableDescriptor
            >(
            Severity.ERROR
        )

    // error goes on ...todo
    @JvmField
    val NONREADONLY_CALL_IN_READONLY_FRAGMENT = DiagnosticFactory0.create<PsiElement>(
        Severity.ERROR
    )

    // This error matches Kotlin's CONFLICTING_OVERLOADS error, except that it renders the
    // annotations with the descriptor. This is important to use for errors where the
    // only difference is whether or not it is annotated with @Fragment or not.
    @JvmField
    var CONFLICTING_OVERLOADS: DiagnosticFactory1<PsiElement, Collection<DeclarationDescriptor>> =
        DiagnosticFactory1.create(
            Severity.ERROR,
            DECLARATION_SIGNATURE_OR_DEFAULT
        )

    // error goes on ...todo
    @JvmField
    val ILLEGAL_TRY_CATCH_AROUND_FRAGMENT =
        DiagnosticFactory0.create<PsiElement>(
            Severity.ERROR
        )

    // This error matches Kotlin's TYPE_MISMATCH error, except that it renders the annotations
    // with the types. This is important to use for type mismatch errors where the only
    // difference is whether or not it is annotated with @Fragment or not.
    @JvmField
    val TYPE_MISMATCH =
        DiagnosticFactory2.create<KtExpression, KotlinType, KotlinType>(
            Severity.ERROR
        )

    init {
        Errors.Initializer.initializeFactoryNamesAndDefaultErrorMessages(
            CanvasErrors::class.java,
            CanvasErrorMessages()
        )
    }
}