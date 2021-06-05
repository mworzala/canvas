@file:JvmName("BasicTest")

package com.mattworzala.canvas

import com.mattworzala.canvas.extra.col
import com.mattworzala.canvas.extra.indices
import com.mattworzala.canvas.extra.row
import com.mattworzala.canvas.extra.all
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import kotlin.math.max
import kotlin.math.min

fun BasicItems() = fragment(9, 5) {
    this[0].item(Material.GOLD_INGOT)

    this[10].item(Material.GOLD_INGOT)

    put(BasicCounter(), 3)

    put(SingleItemFromProps(ItemStack.of(Material.IRON_SHOVEL, 5)), 1)

    put(SingleItemFromProps(ItemStack.of(Material.IRON_HELMET, 5)), 25)

    row(4) {
        item = ItemStack.of(Material.BLACK_STAINED_GLASS_PANE)
    }
    val border = ItemStack.of(Material.BLACK_STAINED_GLASS_PANE)
    col(8) {
        item = border
    }
}

fun SingleItemFromProps(displayItem: ItemStack) = fragment {
    this[0].apply {
        item = displayItem
        onClick {
            println("SingleItem was clicked!!!")
        }
    }
}

fun BasicCounter() = fragment(3) {
    var counter by useState(1)
    item(0, Material.GLOWSTONE_DUST)

    // Decrement
    this[0].apply {
        onClick { counter = max(1, counter - 1) }
        item(Material.RED_CONCRETE) {
            displayName(Component.text("Decrement", NamedTextColor.RED))
        }
    }

    // Counter
    this[1].item(Material.GLOWSTONE_DUST) {
        amount(counter)
    }

    // Increment
    this[0].apply {
        onClick {
            counter = min(64, counter + 1)
        }
        item(Material.GREEN_CONCRETE) {
            displayName(Component.text("Increment", NamedTextColor.GREEN))
        }
    }
}

fun BatchTest() = fragment(9, 2) {
    indices(all) {
        item = ItemStack.of(Material.GREEN_STAINED_GLASS)
    }
}
