package com.mattworzala.canvas.compiler.lower

import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

interface ModuleLoweringPass {
    fun lower(module: IrModuleFragment)
}