package com.mattworzala.canvas

import com.google.common.collect.Queues
import com.mattworzala.canvas.internal.SimpleRenderContext
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import net.minestom.server.event.inventory.InventoryCloseEvent
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.inventory.Inventory
import net.minestom.server.inventory.InventoryType
import net.minestom.server.utils.time.TimeUnit
import java.util.*

/**
 * Represents a renderable document for a player.
 */
//todo needs a lot of cleanup (update task is generally not amazing)
class Canvas(private val player: Player) : SlotHolder {
    private lateinit var inventory: Inventory
    private var root: RenderContext<*>? = null
    private lateinit var items: Array<Slot?>

    override var width: Int = 0
        private set
    override var height: Int = 0
        private set

    private val dirty: Queue<Int> = Queues.newConcurrentLinkedQueue()

    init {
        MinecraftServer.getGlobalEventHandler().addEventCallback(InventoryPreClickEvent::class.java, ::handleClick)
        MinecraftServer.getGlobalEventHandler().addEventCallback(InventoryCloseEvent::class.java, ::handleClose)
        MinecraftServer.getSchedulerManager().buildTask(::update).repeat(1, TimeUnit.TICK).schedule()
    }

    fun <P : Props> draw(component: FunctionComponent<P>, props: P) {
        root?.cleanup()

        inventory = Inventory(InventoryType.CHEST_5_ROW, "Canvas Test")
        width = 9; height = 5
        items = arrayOfNulls(inventory.size)
        root = SimpleRenderContext(this, 0, component)
        (root!! as RenderContext<P>).apply {
            render(props)
            update()
        }

        player.openInventory(inventory)
    }

    private fun update() {
        drawToPlayer()
    }

    private fun drawToPlayer() {
        while (!dirty.isEmpty()) {
            val slot = dirty.poll()
            if (items[slot] == null) continue
            inventory.setItemStack(slot, items[slot]!!.item)
        }
    }

    override fun get(index: Int): Slot {
        dirty.offer(index)
        if (items[index] == null) items[index] = Slot()
        return items[index]!!
    }

    override fun set(index: Int, slot: Slot) {
        dirty.offer(index)
        items[index] = slot
    }

    private fun handleClick(event: InventoryPreClickEvent) {
        if (event.inventory != inventory) return
        if (event.slot >= items.size) return
        event.isCancelled = true

        val unsafeSlot = items[event.slot] ?: return
        unsafeSlot.handleClick(event)
    }

    private fun handleClose(event: InventoryCloseEvent) {
        if (event.inventory != inventory) return
        root?.cleanup()
    }

}