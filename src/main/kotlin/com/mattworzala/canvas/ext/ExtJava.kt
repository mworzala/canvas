package com.mattworzala.canvas.ext

infix fun Int.has(flag: Int) = (this or flag) == flag