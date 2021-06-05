@file:JvmName("DataReworkTest")

package com.mattworzala.canvas

import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material

fun MySmartFragment() = fragment(9, 1) {
    item(0, ItemStack.of(Material.GLOWSTONE_DUST))
}

fun SmartComposition() = fragment(9, 1) {
    put(SingleItem(Material.GLOWSTONE_DUST), 0)
    put(SingleItem(Material.REDSTONE_BLOCK), 1)
}

fun SingleItem(material: Material) = fragment {
    item(0, ItemStack.of(material))
}

//fun MyOtherFragment() = fragment {
//    MyFragment("John")
//
//}
//
//
//fun MyFragment(name: String) = fragment {
//
//}
//
//data class Container(val uid: UniqueId, val func: () -> Unit)
//
//fun createContainer(func: () -> Unit = {}): Container {
//    val uid = StackFrameUniqueId()
//
//    println("Creating container with uid $uid")
//    return Container(uid, func)
//}
//
//fun testA(name: String): Container {
//    return createContainer {
//        testB()
//    }
//}
//
//fun testB() = createContainer {
//    println("Hi")
//}
//
//fun main() {
//    val a = testA("Test A")
//    val b = testA("Test B")
//
////    println(a.uid)
////    println(b.uid)
//}
