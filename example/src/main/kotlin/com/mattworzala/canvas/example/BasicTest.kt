@file:JvmName("BasicTest")

package com.mattworzala.canvas.example

import com.mattworzala.canvas.extra.col
import com.mattworzala.canvas.extra.indices
import com.mattworzala.canvas.extra.row
import com.mattworzala.canvas.extra.all
import com.mattworzala.canvas.fragment
import com.mattworzala.canvas.useEffect
import com.mattworzala.canvas.useState
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.entity.PlayerSkin
import net.minestom.server.inventory.click.ClickType
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.metadata.PlayerHeadMeta
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

fun BasicItems() = fragment(9, 5) {
    this[0].item(Material.GOLD_INGOT)

    this[10].item(Material.GOLD_INGOT)

    put(BasicCounter(), 3)

    put(SingleItemFromProps(ItemStack.of(Material.IRON_SHOVEL, 5)), 1)

    put(SingleItemFromProps(ItemStack.of(Material.IRON_HELMET, 5)), 25)

    item(2, ItemStack.of(Material.PLAYER_HEAD).withMeta(PlayerHeadMeta::class.java) {
        it.skullOwner(UUID.fromString("aceb326f-da15-45bc-bf2f-11940c21780c"))
        it.playerSkin(PlayerSkin.fromUuid("aceb326f-da15-45bc-bf2f-11940c21780c"))
    })
    slot(27) {
        item = ItemStack.of(Material.PLAYER_HEAD).withMeta(PlayerHeadMeta::class.java) {
            it.skullOwner(UUID.fromString("aceb326f-da15-45bc-bf2f-11940c21780c"))
            it.playerSkin(PlayerSkin.fromUuid("aceb326f-da15-45bc-bf2f-11940c21780c"))
        }

        onClick { event ->
            event.player.inventory.addItemStack(ItemStack.of(Material.PLAYER_HEAD).withMeta(PlayerHeadMeta::class.java) {
                it.skullOwner(UUID.fromString("aceb326f-da15-45bc-bf2f-11940c21780c"))
                it.playerSkin(PlayerSkin.fromUuid("aceb326f-da15-45bc-bf2f-11940c21780c"))
            })
        }
    }

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
            println("SingleItem was left clicked!!!")
        }
        onClick(ClickType.RIGHT_CLICK) {
            println("SingleItem was right clicked!!!")
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

fun EffectExample() = fragment(9, 2) {
    var myName by useState("Loading...");

    useEffect(/* No dependencies, so will only execute once on open */) {
        Executors.newSingleThreadScheduledExecutor().schedule({
            println("Loaded name!")
            myName = "John Smith"
        }, 5, TimeUnit.SECONDS)
        null
    }

    slot(0) {
        item = ItemStack.of(Material.REDSTONE)
            .withDisplayName(Component.text(myName))
    }
}
