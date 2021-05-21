/*
 * Copyright 2019 The Android Open Source Project
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

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils.NO_EXPECTED_TYPE
import org.jetbrains.kotlin.types.TypeUtils.UNIT_EXPECTED_TYPE
import org.jetbrains.kotlin.types.typeUtil.replaceAnnotations

object ComposeFqNames {
    private const val root = "com.mattworzala.canvas"
    private const val internalRoot = "$root.internal"
    fun fqNameFor(cname: String) = FqName("$root.$cname")
    fun internalFqNameFor(cname: String) = FqName("$internalRoot.$cname")

    val Fragment = fqNameFor("Fragment")
    val internal = fqNameFor("internal")
    val CurrentFragmentIntrinsic = fqNameFor("<get-currentFragment>")
    val DisallowComposableCalls = fqNameFor("DisallowFragmentCalls")
    val ReadOnlyComposable = fqNameFor("ReadOnlyFragment")
    val ExplicitGroupsComposable = fqNameFor("ExplicitGroupsFragment")
    val NonRestartableComposable = fqNameFor("NonRestartableFragment")
    val composableLambda = internalFqNameFor("composableLambda") //todo
    val composableLambdaInstance = internalFqNameFor("composableLambdaInstance") //todo
    val remember = fqNameFor("remember") //todo
    val key = fqNameFor("key") //todo
    val StableMarker = fqNameFor("StableMarker") //todo
    val Stable = fqNameFor("Stable") //todo
    val Composer = fqNameFor("Composer") //todo
    val ComposeVersion = fqNameFor("ComposeVersion") //todo
    val Package = FqName(root) //todo
    val StabilityInferred = internalFqNameFor("StabilityInferred") //todo
    fun makeComposableAnnotation(module: ModuleDescriptor): AnnotationDescriptor =
        object : AnnotationDescriptor {
            override val type: KotlinType
                get() = module.findClassAcrossModuleDependencies(
                    ClassId.topLevel(Fragment)
                )!!.defaultType
            override val allValueArguments: Map<Name, ConstantValue<*>> get() = emptyMap()
            override val source: SourceElement get() = SourceElement.NO_SOURCE
            override fun toString() = "[@Composable]"
        }
}

fun KotlinType.makeComposable(module: ModuleDescriptor): KotlinType {
    if (hasComposableAnnotation()) return this
    val annotation = ComposeFqNames.makeComposableAnnotation(module)
    return replaceAnnotations(Annotations.create(annotations + annotation))
}

fun IrType.hasComposableAnnotation(): Boolean =
    hasAnnotation(ComposeFqNames.Fragment)

fun IrAnnotationContainer.hasComposableAnnotation(): Boolean =
    hasAnnotation(ComposeFqNames.Fragment)

fun KotlinType.hasComposableAnnotation(): Boolean =
    !isSpecialType && annotations.findAnnotation(ComposeFqNames.Fragment) != null
fun Annotated.hasComposableAnnotation(): Boolean =
    annotations.findAnnotation(ComposeFqNames.Fragment) != null
fun Annotated.hasNonRestartableComposableAnnotation(): Boolean =
    annotations.findAnnotation(ComposeFqNames.NonRestartableComposable) != null
fun Annotated.hasReadonlyFragmentAnnotation(): Boolean =
    annotations.findAnnotation(ComposeFqNames.ReadOnlyComposable) != null
fun Annotated.hasExplicitGroupsAnnotation(): Boolean =
    annotations.findAnnotation(ComposeFqNames.ExplicitGroupsComposable) != null
fun Annotated.hasDisallowComposableCallsAnnotation(): Boolean =
    annotations.findAnnotation(ComposeFqNames.DisallowComposableCalls) != null

internal val KotlinType.isSpecialType: Boolean get() =
    this === NO_EXPECTED_TYPE || this === UNIT_EXPECTED_TYPE

val AnnotationDescriptor.isComposableAnnotation: Boolean get() = fqName == ComposeFqNames.Fragment
