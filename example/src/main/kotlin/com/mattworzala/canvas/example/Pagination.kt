@file:JvmName("Pagination")

package com.mattworzala.canvas.example

import com.mattworzala.canvas.fragment
import com.mattworzala.canvas.useState
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import java.util.concurrent.ThreadLocalRandom

private const val PAGE_SIZE = 9 * 4

fun PagedMenu() = fragment(9, 5) {
    val menuItems by useState(createMenuItems())
    var page by useState(0)

    val pageItems = menuItems
        .drop(page * PAGE_SIZE)
        .take(PAGE_SIZE)

    val delete: (item: Int) -> Unit = {
        !{ menuItems.removeAt((page * PAGE_SIZE) + it) }
    }

    put(MenuPage(pageItems, delete), 0)

    slot(0, 4) {
        item = ItemStack.of(Material.RED_STAINED_GLASS_PANE)
        onClick { page -= 1 }
    }

    slot(8, 4) {
        item = ItemStack.of(Material.GREEN_STAINED_GLASS_PANE)
        onClick { page += 1 }
    }
}

fun MenuPage(page: List<ItemStack>, delete: (item: Int) -> Unit) = fragment(9, 4) {
    for (i in page.indices) {
        put(MenuItem(page[i]) { delete(i) }, i)
    }
}

fun MenuItem(menuItem: ItemStack, delete: () -> Unit) = fragment {
    slot(0) {
        item = menuItem
        onClick {
            owner.sendMessage("Deleting " + item.material.name())
            delete()
        }
    }
}


private fun createMenuItems(): MutableList<ItemStack> {
    val list = mutableListOf<ItemStack>()
    for (i in 0..50) {
        val mat = Material.fromId(ThreadLocalRandom.current().nextInt(0, Material.values().size)) ?: Material.BARREL
        list.add(ItemStack.of(mat))
    }
    return list
}