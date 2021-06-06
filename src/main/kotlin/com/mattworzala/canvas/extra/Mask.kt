package com.mattworzala.canvas.extra

import com.mattworzala.canvas.RenderContext
import com.mattworzala.canvas.Slot
import com.mattworzala.canvas.SlotFunc
import com.mattworzala.canvas.asSlot
import net.minestom.server.item.ItemStack
import net.minestom.server.utils.validate.Check

/**
 * A visual representation of a UI.
 * Masks can either be binary masks or char masks.
 *
 * Binary masks: where 0 is X item, and 1 is Y item.
 *
 * Char masks: where any character can be any item,
 * inspired by the Minecraft recipe format.
 */
class Mask {
    private var binaryFill: Slot? = null
    private var multiFill: MutableMap<Char, Slot>? = null

    var pattern: String? = null
        set(value) { field = value?.filter { it != '\n' } }

    fun fill(fill: ItemStack) {
        if (multiFill != null) throw IllegalStateException("A mask may not be both a binary and multi mask.")
        binaryFill = fill.asSlot()
    }

    fun fill(fill: SlotFunc) {
        if (multiFill != null) throw IllegalStateException("A mask may not be both a binary and multi mask.")
        binaryFill = Slot().apply(fill)
    }

    fun fill(ingredient: Char, item: ItemStack) {
        if (binaryFill != null) throw IllegalStateException("A mask may not be both a binary and multi mask.")
        if (multiFill == null) multiFill = mutableMapOf()
        multiFill!![ingredient] = item.asSlot()
    }

    fun fill(ingredient: Char, slot: SlotFunc) {
        if (binaryFill != null) throw IllegalStateException("A mask may not be both a binary and multi mask.")
        if (multiFill == null) multiFill = mutableMapOf()
        multiFill!![ingredient] = Slot().apply(slot)
    }

    internal fun apply(component: RenderContext) {
        Check.argCondition(component.size != pattern?.length, "Fragment and pattern must be the same size!")

        for (i in 0 until component.size) {
            val char = pattern?.get(i) ?: continue
            if (binaryFill != null) {
                if (char != '0') component[i] = binaryFill!!
            } else {
                val fill = multiFill!![char]
                if (fill != null) component[i] = fill
            }
        }
    }
}

/**
 * Kotlin DSL for masking.
 *
 * @param handler The kotlin DSL handler.
 */
fun RenderContext.mask(handler: Mask.() -> Unit) {
    val mask = Mask()
    mask.handler()
    mask.apply(this)
}