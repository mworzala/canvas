package com.mattworzala.canvas

import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material

typealias ItemFunc = ItemStack.() -> Unit
typealias SlotFunc = Slot.() -> Unit
typealias ClickHandler = Slot.(InventoryPreClickEvent) -> Unit

class Slot internal constructor(
    var item: ItemStack = ItemStack(Material.AIR, 1)
) {
    var onClick: ClickHandler? = null

    fun item(func: ItemFunc) {
        item = ItemStack(Material.AIR, 1)
        item.func()
    }

    fun onClick(handler: ClickHandler) {
        onClick = handler
    }

    fun reset() {
        item = ItemStack(Material.AIR, 1)
        onClick = null
    }

    internal fun handleClick(event: InventoryPreClickEvent) = onClick?.invoke(this, event)
}

interface SlotHolder {
    val width: Int
    val height: Int
    val size: Int get() = width * height

    fun getIndex(x: Int, y: Int) = x + (y * width)

    fun get(index: Int): Slot

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