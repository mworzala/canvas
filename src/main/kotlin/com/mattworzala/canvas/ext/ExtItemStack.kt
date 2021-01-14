package com.mattworzala.canvas.ext

import net.minestom.server.chat.ColoredText
import net.minestom.server.chat.JsonMessage

/* Lore Methods */

fun MutableList<JsonMessage>.plusAssign(text: JsonMessage) { add(text) }

fun MutableList<JsonMessage>.plusAssign(text: String) { add(ColoredText.of(text)) }