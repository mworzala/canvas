@file:JvmName("CanvasV1Kt")

package com.mattworzala.canvas

import com.mattworzala.canvas.internal.SimpleRenderContext
import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue
import it.unimi.dsi.fastutil.ints.IntPriorityQueue
import net.minestom.server.data.Data
import net.minestom.server.entity.Player
import net.minestom.server.event.inventory.InventoryCloseEvent
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.inventory.Inventory
import net.minestom.server.inventory.InventoryType
import net.minestom.server.inventory.click.ClickType

private const val CHEST_INVENTORY_WIDTH = 9

class Canvas internal constructor(private val player: Player) : SlotHolder {

    /**
     * Represents the underlying slots in the inventory. Will match the size of the current value of [container].
     *
     * Empty slots may be represented by null, or a [Slot] with an air item.
     */
    private var items: Array<Slot?> = Array(9) { null }
    private var root: RenderContext? = null

    /**
     * Internal inventory. Externally only used for event checking.
     */
    override var container: Inventory = Inventory(InventoryType.CHEST_1_ROW, "Unnamed Canvas")
        private set

    /**
     * All slots that need to be rerendered and changed.
     */
    private val dirtySlots: IntPriorityQueue = IntArrayFIFOQueue()

    var isViewing: Boolean = false
        private set
    var isViewable: Boolean = true
        private set

    /* Rendering */

    @Synchronized
    fun render(fragment: () -> Fragment) = render(fragment())

    /**
     * Renders a [Fragment] to a player.
     *
     * @param fragment The [Fragment] to render.
     */
    @Synchronized
    internal fun render(fragment: Fragment) {
        // Prep
        val type = getInventoryType(fragment)
        prepareInventory(type)

        // Render fragment
        val newContext = SimpleRenderContext(this, 0, fragment)
        root = newContext
        newContext.render()

        // Force an update so that the inventory is rendered when its opened.
        update()

        // Open inventory (if not open)
        isViewing = true
        if (player.openInventory != container)
            player.openInventory(container)
    }

    /* Slot Holder */

    /** The width of the inventory */
    override val width: Int = CHEST_INVENTORY_WIDTH

    /** The height of the inventory */
    override val height: Int
        get() = container.size / CHEST_INVENTORY_WIDTH

    /**
     * Gets a [Slot] at an inventory [index]
     *
     * @param index The index of the [Slot] you want to get
     *
     * @return The [Slot] at that respective [index]
     */
    override fun get(index: Int): Slot {
        dirtySlots.enqueue(index)
        if (items[index] == null) items[index] = Slot()
        return items[index]!!
    }

    /**
     * Sets a [Slot] at an [index]
     *
     * @param index Where to set the slot.
     * @param slot The slot to set at that position.
     */
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
            container.setItemStack(slot, items[slot]!!.item)
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
        if (index >= container.size) return
        event.isCancelled = true

        //todo handle click types
        if (event.clickType != ClickType.LEFT_CLICK) return

        val slot = this.items[index] ?: return
        slot.onClick?.invoke(slot, event)
    }

    internal fun handleInventoryClose(event: InventoryCloseEvent) = handleClose()

    /* Helpers */

    /**
     * Determines the correct inventory size given a [fragment], and ensures that
     * the fragment meets the sizing criteria to be a root node.
     *
     * @param fragment The fragment to be rendered
     * @return The inventory type (size) required for the given fragment
     * @throws IllegalStateException If the fragment is not a valid root
     */
    private fun getInventoryType(fragment: Fragment): InventoryType {
        if (fragment.width != 9)
            throw IllegalStateException("Canvases cannot directly render components with a width other than $CHEST_INVENTORY_WIDTH.")
        return when (fragment.height) {
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
     * Prepares the [container] to render a new fragment of the given [type].
     * If the inventory is already the correct type, it will reuse it. Otherwise,
     * a new inventory will be created.
     *
     * @param type The type of the inventory required for the fragment
     */
    private fun prepareInventory(type: InventoryType) {
        dirtySlots.clear()
        if (container.inventoryType == type) {
            // Reset data, reuse inventory
            items.fill(null)
            // Mark all slots dirty so they will be re sent during update
            (1 until items.size).forEach(dirtySlots::enqueue)
        } else {
            // New inventory
            container = Inventory(type, "Unnamed Canvas")
            items = arrayOfNulls(type.additionalSlot)
        }
    }

    /**
     * Handles closing the inventory
     * assuming it is currently open for the player.
     */
    private fun handleClose() {
        player.closeInventory()
        root?.cleanup()
        isViewing = false
        root = null
    }
}