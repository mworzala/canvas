package com.mattworzala.canvas.ext

import com.mattworzala.canvas.CanvasProvider
import com.mattworzala.canvas.Canvas
import net.minestom.server.entity.Player

/**
 * An extension for access to a canvas given a Player.
 *
 * This method requires a map access, so caching it is suggested.
 */
val Player.canvas: Canvas
    get() = CanvasProvider.canvas(this)
