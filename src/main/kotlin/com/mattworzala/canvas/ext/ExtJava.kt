package com.mattworzala.canvas.ext

/**
 * Check if a bitmask has a flag.
 *
 * @param flag The flag to cross check through
 *
 * @return If this mask has a flag
 */
infix fun Int.has(flag: Int) = (this or flag) == flag