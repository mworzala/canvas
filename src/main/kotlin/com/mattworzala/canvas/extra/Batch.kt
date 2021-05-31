package com.mattworzala.canvas.extra

import com.mattworzala.canvas.RenderContext
import com.mattworzala.canvas.SlotFunc
import net.minestom.server.utils.validate.Check

/**
 * Sets a list of columns (up and down) on a UI.
 *
 * @param cols The columns to set, starting at 0
 * @param handler The slot DSL handler
 * @return A list of the affected slots
 */
fun RenderContext.col(vararg cols: Int, handler: SlotFunc = {}): List<Int> = apply(cols.flatMap(::col), handler)

/**
 * Sets a list of columns (up and down) on a UI.
 *
 * @param cols The columns to set as a range
 *             (1..5 sets all colums 1, 2, 3, 4, and 5)
 * @param handler The slot DSL handler
 * @return A list of the affected slots
 */
fun RenderContext.col(cols: IntRange, handler: SlotFunc = {}): List<Int> = apply(cols.flatMap(::col), handler)

/**
 * Sets a list of rows (left and right) on a UI.
 *
 * @param rows The rows to set, starting at 0
 * @param handler The slot DSL handler
 * @return A list of the affected slots
 */
fun RenderContext.row(vararg rows: Int, handler: SlotFunc = {}): List<Int> = apply(rows.flatMap(::row), handler)

/**
 * Sets a list of rows (left and right) on a UI.
 *
 * @param rows The rows to set as a range
 *             (1..5 sets all rows 1, 2, 3, 4, and 5)
 * @param handler The slot DSL handler
 * @return A list of the affected slots
 */
fun RenderContext.row(rows: IntRange, handler: SlotFunc = {}): List<Int> = apply(rows.flatMap(::row), handler)

/**
 * Sets a list of indices on a UI.
 *
 * @param indices The indices to set.
 * @param handler The slot DSL handler
 * @return A list of the affected slots
 */
fun RenderContext.indices(vararg indices: Int, handler: SlotFunc = {}): List<Int> = apply(indices.toList(), handler)

/**
 * Sets a list of indices on a UI.
 *
 * @param indices The indices to set.
 * @param handler The slot DSL handler
 * @return A list of the affected slots
 */
fun RenderContext.indices(indices: IntRange, handler: SlotFunc = {}): List<Int> = apply(indices.toList(), handler)

/**
 * Returns a span of the entire fragment.
 */
val RenderContext.all: IntRange
    get() = 0 until (width * height)

/**
 * Sets a column (up and down) on a UI.
 *
 * @param col The column to set
 * @return A list of slot indexes that were set.
 */
internal fun RenderContext.col(col: Int): List<Int> {
    Check.argCondition(col >= width, "Cannot get column outside bounds of fragment.")
    return (0 until height).map { (it * width) + col }
}

/**
 * Sets a column (left and right) on a UI.
 *
 * @param row The row to set
 * @return A list of slot indexes that were set.
 */
internal fun RenderContext.row(row: Int): List<Int> {
    Check.argCondition(row >= height, "Cannot get row outside bounds of fragment.")
    val start = row * width
    return (start until (start + width)).toList()
}