@file:JvmName("CanvasV1Kt")

package com.mattworzala.canvas

import com.mattworzala.canvas.internal.SimpleRenderContext
import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue
import it.unimi.dsi.fastutil.ints.IntPriorityQueue
import net.minestom.server.entity.Player
import net.minestom.server.event.inventory.InventoryCloseEvent
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.inventory.Inventory
import net.minestom.server.inventory.InventoryType
import net.minestom.server.inventory.click.ClickType

private const val CHEST_INVENTORY_WIDTH = 9

//todo need to set Inventory before opening player inventory so close event is not handled
class Canvas internal constructor(private val player: Player) : SlotHolder {
    /**
     * Represents the underlying slots in the inventory. Will match the size of the current value of [inventory].
     *
     * Empty slots may be represented by null, or a [Slot] with an air item.
     */
    private var items: Array<Slot?> = arrayOf()
    private var root: RenderContext<*>? = null

    var inventory: Inventory = Inventory(InventoryType.CHEST_1_ROW, "Unnamed Canvas")
        private set
    private val dirtySlots: IntPriorityQueue = IntArrayFIFOQueue()

    var isViewing: Boolean = false
        private set
    var isViewable: Boolean = true
        private set

    /* Rendering */
    @Synchronized
    fun <P : Props> render(component: Component<P>, props: P) {
        // Prep
        val type = getInventoryType(component)
        prepareInventory(type)

        // Render component
        val newContext = SimpleRenderContext(this, 0, component)
        root = newContext
        newContext.render(props)

        // Force an update so that the inventory is rendered when its opened.
        update()

        // Open inventory (if not open)
        isViewing = true
        if (player.openInventory != inventory)
            player.openInventory(inventory)
    }

    /* Slot Holder */

    override val width: Int = CHEST_INVENTORY_WIDTH
    override val height: Int
        get() = inventory.size / CHEST_INVENTORY_WIDTH

    override fun get(index: Int): Slot {
        dirtySlots.enqueue(index)
        if (items[index] == null) items[index] = Slot()
        return items[index]!!
    }

    override fun set(index: Int, slot: Slot) {
        dirtySlots.enqueue(index)
        items[index] = slot
    }

    /* Lifecycle */

    /**
     * Called once per tick to draw items to the underlying inventory.
     *
     * Also used to update components if [CanvasProvider.useFixedUpdate] is set.
     */
    @Synchronized
    internal fun update() {
        // Send updated slots to viewer.
        while (!dirtySlots.isEmpty) {
            val slot = dirtySlots.dequeueInt()
            if (items[slot] == null) continue
            inventory.setItemStack(slot, items[slot]!!.item)
        }

        // Call root update if fixedUpdate is set.
        if (CanvasProvider.useFixedUpdate)
            root?.update()
    }

    /**
     * Called when the associated player is not longer a valid target.
     */
    internal fun cleanup() {
        handleClose()
        isViewable = false
    }

    /* Event handlers */

    internal fun handleInventoryClick(event: InventoryPreClickEvent) {
        val index = event.slot
        if (index >= inventory.size) return
        event.isCancelled = true

        //todo handle click types
        if (event.clickType != ClickType.LEFT_CLICK) return

        val slot = this.items[index] ?: return
        slot.onClick?.invoke(slot, event)
    }

    internal fun handleInventoryClose(event: InventoryCloseEvent) = handleClose()

    /* Helpers */

    /**
     * Determines the correct inventory size given a [component], and ensures that
     * the component meets the sizing criteria to be a root node.
     *
     * @param component The component to be rendered
     * @return The inventory type (size) required for the given component
     * @throws IllegalStateException If the component is not a valid root
     */
    private fun getInventoryType(component: Component<*>): InventoryType {
        if (component.width != 9)
            throw IllegalStateException("Canvases cannot directly render components with a width other than $CHEST_INVENTORY_WIDTH.")
        return when (component.height) {
            1 -> InventoryType.CHEST_1_ROW
            2 -> InventoryType.CHEST_2_ROW
            3 -> InventoryType.CHEST_3_ROW
            4 -> InventoryType.CHEST_4_ROW
            5 -> InventoryType.CHEST_5_ROW
            6 -> InventoryType.CHEST_6_ROW
            else -> throw IllegalStateException("Canvases cannot render components larger or smaller than an inventory!")
        }
    }

    /**
     * Prepares the [inventory] to render a new component of the given [size].
     * If the inventory is already the correct size, it will reuse it. Otherwise,
     * a new inventory will be created.
     *
     * @param size The size of the inventory required for the component
     */
    private fun prepareInventory(size: InventoryType) {
        dirtySlots.clear()
        if (inventory.inventoryType == size) {
            // Reset data, reuse inventory
            items.fill(null)
            // Mark all slots dirty so they will be re sent during update
            (1 until items.size).forEach(dirtySlots::enqueue)
        } else {
            // New inventory
            inventory = Inventory(size, "Unnamed Canvas")
            items = arrayOfNulls(size.additionalSlot)
        }
    }

    /**
     * Handles closing the inventory assuming it is currently open for the player.
     */
    private fun handleClose() {
        player.closeInventory()
        root?.cleanup()
        isViewing = false
        root = null
    }
}