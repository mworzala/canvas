package com.mattworzala.canvas.compiler.fragment

import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import kotlin.math.ceil


/**
 * An enum of the different "states" a parameter of a composable function can have relating to
 * comparison propagation. Each state is represented by two bits in the `$changed` bitmask.
 */
enum class ParamState(val bits: Int) {
    /**
     * Indicates that nothing is certain about the current state of the parameter. It could be
     * different than it was during the last execution, or it could be the same, but it is not
     * known so the current function looking at it must call equals on it in order to find out.
     * This is the only state that can cause the function to spend slot table space in order to
     * look at it.
     */
    Uncertain(0b000),
    /**
     * This indicates that the value is known to be the same since the last time the function was
     * executed. There is no need to store the value in the slot table in this case because the
     * calling function will *always* know whether the value was the same or different as it was
     * in the previous execution.
     */
    Same(0b001),
    /**
     * This indicates that the value is known to be different since the last time the function
     * was executed. There is no need to store the value in the slot table in this case because
     * the calling function will *always* know whether the value was the same or different as it
     * was in the previous execution.
     */
    Different(0b010),
    /**
     * This indicates that the value is known to *never change* for the duration of the running
     * program.
     */
    Static(0b011),
    Unknown(0b100),
    Mask(0b111);

    fun bitsForSlot(slot: Int): Int = bitsForSlot(bits, slot)
}

const val BITS_PER_INT = 31
const val SLOTS_PER_INT = 10
const val BITS_PER_SLOT = 3

fun bitsForSlot(bits: Int, slot: Int): Int {
    val realSlot = slot.rem(SLOTS_PER_INT)
    return bits shl (realSlot * BITS_PER_SLOT + 1)
}

fun defaultsParamIndex(index: Int): Int = index / BITS_PER_INT
fun defaultsBitIndex(index: Int): Int = index.rem(BITS_PER_INT)

val IrFunction.thisParamCount
    get() = (if (dispatchReceiverParameter != null) 1 else 0) +
            (if (extensionReceiverParameter != null) 1 else 0)

fun changedParamCount(realValueParams: Int, thisParams: Int): Int {
    if (realValueParams == 0) return 1
    val totalParams = realValueParams + thisParams
    return ceil(
        totalParams.toDouble() / SLOTS_PER_INT.toDouble()
    ).toInt()
}

fun changedParamCountFromTotal(totalParamsIncludingThisParams: Int): Int {
    var realParams = totalParamsIncludingThisParams
    realParams-- // composer param
    realParams-- // first changed param (always present)
    var changedParams = 0
    do {
        realParams -= SLOTS_PER_INT
        changedParams++
    } while (realParams > 0)
    return changedParams
}

fun defaultParamCount(realValueParams: Int): Int {
    return ceil(
        realValueParams.toDouble() / BITS_PER_INT.toDouble()
    ).toInt()
}

fun fragmentSyntheticParamCount(
    realValueParams: Int,
    thisParams: Int = 0,
    hasDefaults: Boolean = false
): Int {
    return 1 + // composer param
            changedParamCount(realValueParams, thisParams) +
            if (hasDefaults) defaultParamCount(realValueParams) else 0
}

interface IrChangedBitMaskValue {
    val used: Boolean
    fun irLowBit(): IrExpression
    fun irIsolateBitsAtSlot(slot: Int, includeStableBit: Boolean): IrExpression
    fun irSlotAnd(slot: Int, bits: Int): IrExpression
    fun irHasDifferences(usedParams: BooleanArray): IrExpression
    fun irCopyToTemporary(
        nameHint: String? = null,
        isVar: Boolean = false,
        exactName: Boolean = false
    ): IrChangedBitMaskVariable
    fun putAsValueArgumentInWithLowBit(
        fn: IrFunctionAccessExpression,
        startIndex: Int,
        lowBit: Boolean
    )
    fun irShiftBits(fromSlot: Int, toSlot: Int): IrExpression
}

interface IrDefaultBitMaskValue {
    fun irIsolateBitAtIndex(index: Int): IrExpression
    fun irHasAnyProvidedAndUnstable(unstable: BooleanArray): IrExpression
    fun putAsValueArgumentIn(fn: IrFunctionAccessExpression, startIndex: Int)
}

interface IrChangedBitMaskVariable : IrChangedBitMaskValue {
    fun asStatements(): List<IrStatement>
    fun irOrSetBitsAtSlot(slot: Int, value: IrExpression): IrExpression
    fun irSetSlotUncertain(slot: Int): IrExpression
}