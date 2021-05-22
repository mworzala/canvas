package com.mattworzala.canvas.ext

import net.kyori.adventure.text.Component
import net.minestom.server.inventory.Inventory

interface InventoryHandle {
    val handle: Inventory

    var title: Component
        get() = handle.title
        set(value) { handle.title = value }

    var sTitle: String
        get() = throw IllegalAccessException("String title should only be set.")
        set(value) { title = Component.text(value) }
}