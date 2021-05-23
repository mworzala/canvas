package com.mattworzala.canvas.internal

import com.mattworzala.canvas.FragmentContext

private const val SLOTS_PER_INT = 10
private const val BITS_PER_SLOT = 3

internal fun bitsForSlot(bits: Int, slot: Int): Int {
    val realSlot = slot.rem(SLOTS_PER_INT)
    return bits shl (realSlot * BITS_PER_SLOT + 1)
}

internal fun sameBits(slot: Int): Int = bitsForSlot(0b01, slot)
internal fun differentBits(slot: Int): Int = bitsForSlot(0b10, slot)

class FragmentLambdaImpl(
    key: Int,
    tracked: Boolean,
    sourceInformation: String?
) : FragmentLambda() {
    fun update(block: Any) {
        //todo
    }
}

open class FragmentLambda {
    operator fun invoke(c: FragmentContext, changed: Int): Any? = null //todo

    operator fun invoke(p1: Any?, c: FragmentContext, changed: Int): Any? = null //todo

    operator fun invoke(p1: Any?, p2: Any?, c: FragmentContext, changed: Int): Any? = null //todo

    operator fun invoke(p1: Any?, p2: Any?, p3: Any?, c: FragmentContext, changed: Int): Any? = null //todo

    operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        c: FragmentContext,
        changed: Int
    ): Any? = null //todo

    operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        c: FragmentContext,
        changed: Int
    ): Any? = null //todo

    operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        c: FragmentContext,
        changed: Int
    ): Any? = null //todo

    operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        c: FragmentContext,
        changed: Int
    ): Any? = null //todo

    operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        c: FragmentContext,
        changed: Int
    ): Any? = null //todo

    operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?,
        c: FragmentContext,
        changed: Int
    ): Any? = null //todo

    operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?,
        p10: Any?,
        c: FragmentContext,
        changed: Int,
        changed1: Int
    ): Any? = null //todo

    operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?,
        p10: Any?,
        p11: Any?,
        c: FragmentContext,
        changed: Int,
        changed1: Int
    ): Any? = null //todo

    operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?,
        p10: Any?,
        p11: Any?,
        p12: Any?,
        c: FragmentContext,
        changed: Int,
        changed1: Int
    ): Any? = null //todo

    operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?,
        p10: Any?,
        p11: Any?,
        p12: Any?,
        p13: Any?,
        c: FragmentContext,
        changed: Int,
        changed1: Int
    ): Any? = null //todo

    operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?,
        p10: Any?,
        p11: Any?,
        p12: Any?,
        p13: Any?,
        p14: Any?,
        c: FragmentContext,
        changed: Int,
        changed1: Int
    ): Any? = null //todo

    operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?,
        p10: Any?,
        p11: Any?,
        p12: Any?,
        p13: Any?,
        p14: Any?,
        p15: Any?,
        c: FragmentContext,
        changed: Int,
        changed1: Int
    ): Any? = null //todo

    operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?,
        p10: Any?,
        p11: Any?,
        p12: Any?,
        p13: Any?,
        p14: Any?,
        p15: Any?,
        p16: Any?,
        c: FragmentContext,
        changed: Int,
        changed1: Int
    ): Any? = null //todo

    operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?,
        p10: Any?,
        p11: Any?,
        p12: Any?,
        p13: Any?,
        p14: Any?,
        p15: Any?,
        p16: Any?,
        p17: Any?,
        c: FragmentContext,
        changed: Int,
        changed1: Int
    ): Any? = null //todo

    operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?,
        p10: Any?,
        p11: Any?,
        p12: Any?,
        p13: Any?,
        p14: Any?,
        p15: Any?,
        p16: Any?,
        p17: Any?,
        p18: Any?,
        c: FragmentContext,
        changed: Int,
        changed1: Int
    ): Any? = null //todo
}

fun fragmentLambda(
    fragmentContext: FragmentContext,
    key: Int,
    tracked: Boolean,
    sourceInformation: String?,
    block: Any
): FragmentLambda {
    //todo
    return FragmentLambdaImpl(key, tracked, sourceInformation)
}

fun fragmentLambdaInstance(
    key: Int,
    tracked: Boolean,
    sourceInformation: String?,
    block: Any
): FragmentLambda =
    //todo
    FragmentLambdaImpl(key, tracked, sourceInformation).apply { update(block) }