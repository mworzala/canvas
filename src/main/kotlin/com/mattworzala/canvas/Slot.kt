package com.mattworzala.canvas

import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material

typealias ItemFunc = ItemStack.() -> Unit
typealias SlotFunc = Slot.() -> Unit
typealias ClickHandler = Slot.(InventoryPreClickEvent) -> Unit

/**
 * A slot, which contains clickers + the item inside.
 */
class Slot internal constructor(
    /**
     * The item in the slot
     */
    var item: ItemStack = ItemStack(Material.AIR, 1)
) {
    /**
     * The clicker in the slot.
     * Will trigger when someone clicks on the slot.
     */
    var onClick: ClickHandler? = null

    /**
     * Kotlin DSL for defining item properties
     *
     * @param func The item function for property decleration.
     */
    fun item(func: ItemFunc) {
        item = ItemStack(Material.AIR, 1)
        item.func()
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
        item = ItemStack(Material.AIR, 1)
        onClick = null
    }
}

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
    fun get(index: Int): Slot

    /**
     * Sets a slot to an index
     *
     * @param index The index where the slot should be
     * @param slot The slot to place at that [index]
     */
    fun set(index: Int, slot: Slot)

    fun apply(slots: List<Int>, handler: SlotFunc): List<Int> {
        slots.map(::get).forEach(handler)
        return slots
    }

    /* Nicer Syntax */

    fun slot(x: Int, y: Int, handler: SlotFunc) = slot(getIndex(x, y), handler)

    fun slot(index: Int, handler: SlotFunc) = handler(get(index))

    fun item(x: Int, y: Int, handler: ItemFunc) = item(getIndex(x, y), handler)

    fun item(index: Int, handler: ItemFunc) = get(index).item(handler)
}

internal fun ItemStack.asSlot(): Slot = Slot(this)