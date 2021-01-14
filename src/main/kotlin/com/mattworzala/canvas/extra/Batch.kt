package com.mattworzala.canvas.extra

import com.mattworzala.canvas.Component
import com.mattworzala.canvas.SlotFunc
import net.minestom.server.utils.validate.Check

fun Component<*>.col(vararg cols: Int, handler: SlotFunc = {}): List<Int> = apply(cols.flatMap(::col), handler)

fun Component<*>.col(cols: IntRange, handler: SlotFunc = {}): List<Int> = apply(cols.flatMap(::col), handler)


fun Component<*>.row(vararg rows: Int, handler: SlotFunc = {}): List<Int> = apply(rows.flatMap(::row), handler)

fun Component<*>.row(rows: IntRange, handler: SlotFunc = {}): List<Int> = apply(rows.flatMap(::row), handler)


internal fun Component<*>.col(col: Int): List<Int> {
    Check.argCondition(col >= width, "Cannot get column outside bounds of component.")
    return (0 until height).map { (it * width) + col }
}

internal fun Component<*>.row(row: Int): List<Int> {
    Check.argCondition(row >= height, "Cannot get row outside bounds of component.")
    val start = row * width
    return (start until (start + width)).toList()
}