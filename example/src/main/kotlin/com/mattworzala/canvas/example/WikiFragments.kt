@file:JvmName("WikiFragments")

package com.mattworzala.canvas.example

import com.mattworzala.canvas.ext.InventoryHandle
import com.mattworzala.canvas.fragment
import com.mattworzala.canvas.useState
import net.kyori.adventure.text.Component
import net.minestom.server.item.Material
import java.util.concurrent.ThreadLocalRandom

fun FirstFragment() = fragment(width = 9, height = 1) {
    item(0, Material.GLOWSTONE_DUST)
}

fun FirstListener() = fragment(9, 1) {
    slot(0) {
        item(Material.GLOWSTONE_DUST)
        onClick {
            println("Glowstone dust was clicked!")
        }
    }
}

fun Composition() = fragment(9, 2) {
    put(FirstFragment(), 0)
    put(FirstListener(), 1 * width)
}

fun TitledFragment() = fragment(width = 9, height = 1) {
    inventory.title = Component.text("Titled Inventory")

    item(0, Material.GLOWSTONE_DUST)
}

fun WithData(name: String = "Canvas") = fragment(1, 1) {
    item(0, Material.PAPER) {
        displayName(Component.text("Hello, $name"))
    }
}

fun CompositionWithData() = fragment(9, 1) {
    put(WithData(), 0)
    put(WithData("Michael"), 1)
}

fun WithState() = fragment(1, 1) {
    var number by useState(-1)

    val displayString = if (number == -1) "Click for number!" else "Number: $number"
    slot(0) {
        item(Material.ENDER_PEARL) {
            displayName(Component.text(displayString))
        }
        onClick {
            number = ThreadLocalRandom.current().nextInt()
        }
    }
}

class MyMutableState {
    var number: Int = -1
}

fun WithMutableState() = fragment {
    var mutable by useState(MyMutableState())

    val displayString = if (mutable.number == -1) "Click for number!" else "Number: ${mutable.number}"
    slot(0) {
        item(Material.ENDER_PEARL) {
            displayName(Component.text(displayString))
        }
        onClick {
            !{ mutable.number = ThreadLocalRandom.current().nextInt() }
        }
    }
}

fun CompositionWithState() = fragment(9, 1) {
    put(WithState(), 0)
    put(WithState(), 1)
    put(WithMutableState(), 2)
}

