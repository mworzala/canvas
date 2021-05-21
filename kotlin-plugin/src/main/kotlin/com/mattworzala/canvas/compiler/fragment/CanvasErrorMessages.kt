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

package com.mattworzala.canvas.compiler.fragment

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.Renderers
import org.jetbrains.kotlin.diagnostics.rendering.Renderers.RENDER_TYPE_WITH_ANNOTATIONS

@AutoService(DefaultErrorMessages.Extension::class)
class CanvasErrorMessages : DefaultErrorMessages.Extension {
    private val MAP =
        DiagnosticFactoryToRendererMap(
            "Fragment"
        )
    override fun getMap() = MAP

    init {
        MAP.put(
            CanvasErrors.FRAGMENT_INVOCATION,
            "@Fragment invocations can only happen from the context of a @Fragment function"
        )

        MAP.put(
            CanvasErrors.FRAGMENT_EXPECTED,
            "Functions which invoke @Fragment functions must be marked with the @Fragment " +
                "annotation"
        )

        MAP.put(
            CanvasErrors.FRAGMENT_FUNCTION_REFERENCE,
            "Function References of @Fragment functions are not currently supported"
        )

        MAP.put(
            CanvasErrors.CAPTURED_FRAGMENT_INVOCATION,
            "Fragment calls are not allowed inside the {0} parameter of {1}",
            Renderers.NAME,
            Renderers.COMPACT
        )

        MAP.put(
            CanvasErrors.MISSING_DISALLOW_FRAGMENT_CALLS_ANNOTATION,
            "Parameter {0} cannot be inlined inside of lambda argument {1} of {2} " +
                "without also being annotated with @DisallowFragmentCalls", //todo DisallowFragmentCalls?
            Renderers.NAME,
            Renderers.NAME,
            Renderers.NAME
        )

        MAP.put(
            CanvasErrors.NONREADONLY_CALL_IN_READONLY_FRAGMENT,
            "Fragments marked with @ReadOnlyFragment can only call other @ReadOnlyFragment " +
                "fragments" //todo ReadOnlyFragment
        )

        MAP.put(
            CanvasErrors.FRAGMENT_PROPERTY_BACKING_FIELD,
            "Fragment properties are not able to have backing fields"
        )

        MAP.put(
            CanvasErrors.CONFLICTING_OVERLOADS,
            "Conflicting overloads: {0}",
            Renderers.commaSeparated(
                Renderers.FQ_NAMES_IN_TYPES_WITH_ANNOTATIONS
            )
        )

        MAP.put(
            CanvasErrors.FRAGMENT_VAR,
            "Fragment properties are not able to have backing fields"
        )
        MAP.put(
            CanvasErrors.FRAGMENT_SUSPEND_FUN,
            "Fragment properties are not able to have backing fields"
        )
        MAP.put(
            CanvasErrors.FRAGMENT_FUN_MAIN,
            "Fragment main functions are not currently supported"
        )
        MAP.put(
            CanvasErrors.ILLEGAL_TRY_CATCH_AROUND_FRAGMENT,
            "Try catch is not supported around fragment function invocations."
        )
        MAP.put(
            CanvasErrors.TYPE_MISMATCH,
            "Type inference failed. Expected type mismatch: inferred type is {1} but {0}" +
                " was expected",
            RENDER_TYPE_WITH_ANNOTATIONS,
            RENDER_TYPE_WITH_ANNOTATIONS
        )
    }
}