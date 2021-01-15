package com.mattworzala.canvas.extra

import com.mattworzala.canvas.RenderContext
import com.mattworzala.canvas.SlotFunc
import net.minestom.server.utils.validate.Check

fun RenderContext<*>.col(vararg cols: Int, handler: SlotFunc = {}): List<Int> = apply(cols.flatMap(::col), handler)

fun RenderContext<*>.col(cols: IntRange, handler: SlotFunc = {}): List<Int> = apply(cols.flatMap(::col), handler)


fun RenderContext<*>.row(vararg rows: Int, handler: SlotFunc = {}): List<Int> = apply(rows.flatMap(::row), handler)

fun RenderContext<*>.row(rows: IntRange, handler: SlotFunc = {}): List<Int> = apply(rows.flatMap(::row), handler)


internal fun RenderContext<*>.col(col: Int): List<Int> {
    Check.argCondition(col >= width, "Cannot get column outside bounds of component.")
    return (0 until height).map { (it * width) + col }
}

internal fun RenderContext<*>.row(row: Int): List<Int> {
    Check.argCondition(row >= height, "Cannot get row outside bounds of component.")
    val start = row * width
    return (start until (start + width)).toList()
}