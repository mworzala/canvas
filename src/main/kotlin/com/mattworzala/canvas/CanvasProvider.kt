package com.mattworzala.canvas

import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import net.minestom.server.event.inventory.InventoryCloseEvent
import net.minestom.server.event.inventory.InventoryOpenEvent
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.event.player.PlayerDisconnectEvent
import net.minestom.server.utils.time.TimeUnit
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * An object for providing all canvas instances.
 * Stores an internal ConcurrentHashMap for thread safety.
 */
object CanvasProvider {
    /** Internal map of all canvases. */
    private val canvases: MutableMap<UUID, Canvas> = ConcurrentHashMap()

    internal var useFixedUpdate: Boolean = false
        private set //todo not implemented

    @JvmStatic
    fun canvas(player: Player): Canvas = canvases[player.uuid] ?: createCanvas(player)

    /* Options */

    @JvmStatic
    fun useFixedUpdate() {
        useFixedUpdate = true
    }

    private fun createCanvas(player: Player) = Canvas(player).apply {
        canvases[player.uuid] = this
    }

    /* Event handing */

    private fun handleInventoryOpen(event: InventoryOpenEvent) {
        // unused as of now
    }

    private fun handleInventoryClick(event: InventoryPreClickEvent) {
        canvases.values
            .filter { it.container == event.inventory }
            .forEach { it.handleInventoryClick(event) }
    }

    private fun handleInventoryClose(event: InventoryCloseEvent) {
        canvases.values
            .filter { it.container == event.inventory }
            .forEach { it.handleInventoryClose(event) }
    }

    private fun handlePlayerDisconnect(event: PlayerDisconnectEvent) {
        val uuid = event.player.uuid
        canvases[uuid]?.cleanup()
        canvases.remove(uuid)
    }

    init {
//        MinecraftServer.getGlobalEventHandler().addEventCallback(InventoryOpenEvent::class.java, ::handleInventoryOpen)
        MinecraftServer.getGlobalEventHandler().addEventCallback(InventoryPreClickEvent::class.java, ::handleInventoryClick)
        MinecraftServer.getGlobalEventHandler().addEventCallback(InventoryCloseEvent::class.java, ::handleInventoryClose)

        MinecraftServer.getGlobalEventHandler().addEventCallback(PlayerDisconnectEvent::class.java, ::handlePlayerDisconnect)

        // Fixed update task
        MinecraftServer.getSchedulerManager().buildTask {
            canvases.values.forEach(Canvas::update)
        }.repeat(1, TimeUnit.TICK).schedule()
    }
}

