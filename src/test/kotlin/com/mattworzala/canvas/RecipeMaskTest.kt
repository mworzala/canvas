@file:JvmName("RecipeMaskTest")

package com.mattworzala.canvas

import com.mattworzala.canvas.extra.mask
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material

fun RecipeMaskTest() = fragment(9, 5) {

    mask {
        pattern = """
            BBBBBBBBB
            BWWWWWWWB
            BWW___WWB
            BWWWWWWWB
            BBBBBBBBB
        """.trimIndent()

        !{
            // My code here
        }

        fill('B') {
            item = ItemStack.of(Material.BLACK_STAINED_GLASS_PANE)
            onClick {
                println("Clicked on 'B' slot!")
            }
        }

        fill('W') {
            item = ItemStack.of(Material.WHITE_STAINED_GLASS_PANE)
            onClick {
                println("Clicked on 'W' slot!")
            }
        }

        fill('_', ItemStack.AIR)
    }
}

