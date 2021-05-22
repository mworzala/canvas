@file:JvmName("BasicTest")

package com.mattworzala.canvas

import com.mattworzala.canvas.extra.col
import com.mattworzala.canvas.extra.row
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import kotlin.math.max
import kotlin.math.min

@JvmField
val BasicItems = fragment(9, 5) {
    this[0].item(Material.GOLD_INGOT)

    this[10].item(Material.GOLD_INGOT)

    put(BasicCounter, 3)

    put(SingleItemFromProps, 1) {
        this["item"] = ItemStack.of(Material.IRON_SHOVEL, 5)
    }

    put(SingleItemFromProps, 25) {
        this["item"] = ItemStack.of(Material.IRON_HELMET, 5)
    }

    row(4) {
        item = ItemStack.of(Material.BLACK_STAINED_GLASS_PANE)
    }
    val border = ItemStack.of(Material.BLACK_STAINED_GLASS_PANE)
    col(8) {
        item = border
    }
}

@JvmField
val SingleItemFromProps = fragment {
    this[0].apply {
        item = data["item"]!!
        onClick {
            println("SingleItem was clicked!!!")
        }
    }
}

@JvmField
val BasicCounter = fragment(3) {
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

