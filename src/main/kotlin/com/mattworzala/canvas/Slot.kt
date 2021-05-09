package com.mattworzala.canvas

import com.mattworzala.canvas.extra.col
import com.mattworzala.canvas.extra.row
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.item.ItemStack
import net.minestom.server.item.ItemStackBuilder
import net.minestom.server.item.Material

typealias ItemFunc = ItemStackBuilder.() -> Unit
typealias SlotFunc = Slot.() -> Unit
typealias ClickHandler = Slot.(InventoryPreClickEvent) -> Unit

/**
 * A slot, which contains click handlers + the item inside.
 */
class Slot internal constructor(
    /**
     * The item in the slot
     */
    var item: ItemStack = ItemStack.AIR
) {
    /**
     * The clicker in the slot.
     * Will trigger when someone clicks on the slot.
     */
    var onClick: ClickHandler? = null
        private set

    /**
     * Kotlin DSL for defining item properties
     *
     * @param func The item function for property decleration.
     */
    fun item(material: Material, func: ItemFunc = {}) {
        val itemBuilder = ItemStack.builder(material)
        itemBuilder.func()
        item = itemBuilder.build()
    }

    /**
     * Kotlin DSL which triggers when someone clicks on that slot.
     *
     * @param handler The lambda (triggers during a click)
     */
    fun onClick(handler: ClickHandler) {
        onClick = handler
    }

    /**
     * Resets the item to air and the clicker to nothing.
     */
    fun reset() {
        item = ItemStack.AIR
        onClick = null
    }
}

/**
 * Represents a container which has slots and manipulation of those slots.
 */
interface SlotHolder {
    val width: Int
    val height: Int
    val size: Int get() = width * height

    fun getIndex(x: Int, y: Int) = x + (y * width)

    /**
     * Gets a slot from an index.
     *
     * @param index The index of the slot.
     *
     * @return The slot that is in that [index]
     */
    operator fun get(index: Int): Slot

    /**
     * Sets a slot to an index
     *
     * @param index The index where the slot should be
     * @param slot The slot to place at that [index]
     */
    operator fun set(index: Int, slot: Slot)

    /**
     * Used to apply the same slot to a group of slots in the inventory.
     *
     * To perform actions on rows/columns, see [RenderContext.col] and [RenderContext.row].
     *
     * @param slots The indices to apply the slot function
     * @param handler The DSL to apply
     */
    fun apply(slots: List<Int>, handler: SlotFunc): List<Int> {
        slots.map(::get).forEach(handler)
        return slots
    }

    /* Nicer Syntax */

    fun slot(x: Int, y: Int, handler: SlotFunc) = slot(getIndex(x, y), handler)

    fun slot(index: Int, handler: SlotFunc) = handler(get(index))

    fun item(x: Int, y: Int, material: Material, handler: ItemFunc = {}) = item(getIndex(x, y), material, handler)

    fun item(index: Int, material: Material, handler: ItemFunc = {}) = get(index).item(material, handler)
}

internal fun ItemStack.asSlot(): Slot = Slot(this)