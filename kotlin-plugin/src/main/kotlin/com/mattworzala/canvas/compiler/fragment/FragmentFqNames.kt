package com.mattworzala.canvas.compiler.fragment

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
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.typeUtil.replaceAnnotations

object FragmentFqNames {
    private const val root = "com.mattworzala.canvas"
    private const val internalRoot = "$root.internal"
    fun fqNameFor(cname: String) = FqName("$root.$cname")
    fun internalFqNameFor(cname: String) = FqName("$internalRoot.$cname")

    val Fragment = fqNameFor("Fragment")
    val FragmentContext = fqNameFor("FragmentContext")
    val ContextIntrinsic = internalFqNameFor("<get-currentFragmentContext>")

    fun makeFragmentAnnotation(module: ModuleDescriptor): AnnotationDescriptor =
        object : AnnotationDescriptor {
            override val type: KotlinType
                get() = module.findClassAcrossModuleDependencies(ClassId.topLevel(Fragment))!!.defaultType
            override val allValueArguments: Map<Name, ConstantValue<*>> get() = emptyMap()
            override val source: SourceElement get() = SourceElement.NO_SOURCE
            override fun toString() = "[@Fragment]"
        }
}

fun KotlinType.makeFragment(module: ModuleDescriptor): KotlinType {
    if (hasFragmentAnnotation()) return this
    val annotation = FragmentFqNames.makeFragmentAnnotation(module)
    return replaceAnnotations(Annotations.create(annotations + annotation))
}

fun IrType.hasFragmentAnnotation(): Boolean =
    hasAnnotation(FragmentFqNames.Fragment)

fun IrAnnotationContainer.hasFragmentAnnotation(): Boolean =
    hasAnnotation(FragmentFqNames.Fragment)

fun KotlinType.hasFragmentAnnotation(): Boolean =
    !isSpecialType && annotations.findAnnotation(FragmentFqNames.Fragment) != null

fun Annotated.hasFragmentAnnotation(): Boolean =
    annotations.findAnnotation(FragmentFqNames.Fragment) != null

internal val KotlinType.isSpecialType: Boolean get() =
    this === TypeUtils.NO_EXPECTED_TYPE || this === TypeUtils.UNIT_EXPECTED_TYPE

val AnnotationDescriptor.isFragmentAnnotation: Boolean get() = fqName == FragmentFqNames.Fragment
