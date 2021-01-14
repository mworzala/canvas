package com.mattworzala.canvas.extra

import com.mattworzala.canvas.Component
import com.mattworzala.canvas.asSlot
import net.minestom.server.item.ItemStack
import net.minestom.server.utils.validate.Check

class Mask {
    private var binaryFill: ItemStack? = null
    private var multiFill: MutableMap<Char, ItemStack>? = null;

    var pattern: String? = null
        set(value) { field = value?.filter { it != '\n' } }

    fun fill(fill: ItemStack) {
        if (multiFill != null) throw IllegalStateException("A mask may not be both a binary and multi mask.")
        binaryFill = fill
    }

    fun fill(fill: Pair<Char, ItemStack>) {
        if (binaryFill != null) throw IllegalStateException("A mask may not be both a binary and multi mask.")
        if (multiFill == null) multiFill = mutableMapOf()
        multiFill!![fill.first] = fill.second
    }

    infix fun Char.with(item: ItemStack): Pair<Char, ItemStack> = this to item

    internal fun apply(component: Component<*>) {
        Check.argCondition(component.size != pattern?.length, "Component and pattern must be the same size!")

        for (i in 0 until component.size) {
            val char = pattern!![i]
            if (binaryFill != null) {
                if (char != '0') component.set(i, binaryFill!!.asSlot())
            } else {
                val fill = multiFill!![char]
                if (fill != null) component.set(i, fill.asSlot())
            }
        }
    }
}

fun Component<*>.mask(handler: Mask.() -> Unit) {
    val mask = Mask()
    mask.handler()
    mask.apply(this)
}