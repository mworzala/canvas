package com.mattworzala.canvas.compiler.fragment

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace

class FragmentIrGenerationExtension : IrGenerationExtension {
    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val bindingTrace = DelegatingBindingTrace(
            pluginContext.bindingContext,
            "trace in FragmentIrGenerationExtension"
        )

        // create a symbol remapper to be used across all transforms
        val symbolRemapper = FragmentSymbolRemapper();

        moduleFragment.annotateFragmentFunctions(pluginContext)

        // There was LiveLiterals here, but i dont think its necessary

        //todo fun interface lowering, but may not be required. needs investigation
//        FragmentFunInterfaceLowering(pluginContext).lower(moduleFragment)

        // Memoize normal lambdas and wrap fragment lambdas
//        FragmentLambdaMemoization(pluginContext, symbolRemapper, bindingTrace)
//            .lower(moduleFragment)

        //todo decoys

        // transform all fragment functions to have an extra synthetic fragment context
        // parameter. this will also transform all types and calls to include the extra
        // parameter.
        FragmentParamTransformer(
            pluginContext,
            symbolRemapper,
            bindingTrace
        ).lower(moduleFragment)

        // transform calls to the currentFragmentContext to just use the local parameter from the
        // previous transform
        ContextIntrinsicTransformer(pluginContext).lower(moduleFragment)

        //todo function body transformer (REQUIRES PARAM TRANSFORMER)

        moduleFragment.patchDeclarationParents()
        println(moduleFragment.dump())
        println(moduleFragment.dumpSrc())
    }
}