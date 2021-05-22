@file:JvmName("WikiFragments")

package com.mattworzala.canvas

import net.kyori.adventure.text.Component
import net.minestom.server.item.Material
import java.util.concurrent.ThreadLocalRandom

@JvmField
val FirstFragment = fragment(width = 9, height = 1) {
    item(0, Material.GLOWSTONE_DUST)
}

@JvmField
val FirstListener = fragment(9, 1) {
    slot(0) {
        item(Material.GLOWSTONE_DUST)
        onClick {
            println("Glowstone dust was clicked!")
        }
    }
}

@JvmField
val Composition = fragment(9, 2) {
    put(FirstFragment, 0)
    put(FirstListener, 1 * width)
}

@JvmField
val WithData = fragment(1, 1) {
    val name: String = data.get("name") ?: "Canvas"

    item(0, Material.PAPER) {
        displayName(Component.text("Hello, $name"))
    }
}

@JvmField
val CompositionWithData = fragment(9, 1) {
    put(WithData, 0)
    put(WithData, 1) {
        set("name", "Michael")
    }
}

@JvmField
val WithState = fragment(1, 1) {
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

@JvmField
val WithMutableState = fragment {
    var mutable by useState(MyMutableState())

    val displayString = if (mutable.number == -1) "Click for number!" else "Number: ${mutable.number}"
    slot(0) {
        item(Material.ENDER_PEARL) {
            displayName(Component.text(displayString))
        }
        onClick {
            mutate {
                mutable.number = ThreadLocalRandom.current().nextInt()
            }
        }
    }
}

@JvmField
val CompositionWithState = fragment(9, 1) {
    put(WithState, 0)
    put(WithState, 1)
    put(WithMutableState, 2)
}

