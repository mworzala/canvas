package com.mattworzala.canvas.internal

import java.lang.RuntimeException

/**
 * TODO
 *
 */
interface UniqueId

/**
 * TODO
 *
 * @property key
 */
class HashCodeUniqueId(private val key: Any) : UniqueId {
    override fun equals(other: Any?): Boolean = hashCode() == other.hashCode()

    override fun hashCode(): Int = key.hashCode()

    override fun toString(): String = "UniqueId($key, ${hashCode()})"
}

/**
 * TODO
 *
 */
class StackFrameUniqueId : UniqueId {
    private val callId: String

    init {
        val frame = StackWalker.getInstance().walk {
            return@walk it.skip(/*skip init, direct caller, fragment method*/ 5).findFirst().orElse(null)
        } ?: throw RuntimeException("Unable to reach instantiating stack frame, must switch to a hashcode based uid.") //todo improve this handling (could auto switch)
        callId = "${frame.className}.${frame.methodName}${frame.descriptor}.${frame.lineNumber}";
    }

    override fun equals(other: Any?): Boolean = hashCode() == other.hashCode()

    override fun hashCode(): Int = callId.hashCode()

    override fun toString(): String = "UniqueId($callId, ${hashCode()})"
}