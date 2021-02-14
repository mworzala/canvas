package com.mattworzala.canvas

import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player

//todo should not be the same :P
fun propsOf(): Props = mutablePropsOf()
fun mutablePropsOf() = MutableProps(MinecraftServer.getConnectionManager().onlinePlayers.first())

interface Props {
    val player: Player

    operator fun <T> get(prop: String): T
    operator fun <T> get(prop: String, default: T): T
}

@Suppress("UNCHECKED_CAST")
class MutableProps(
    override val player: Player
) : Props {
    private val props: MutableMap<String, Any?> = mutableMapOf();

    override fun <T> get(prop: String): T = props[prop] as T
    override fun <T> get(prop: String, default: T): T = if (prop in props) get(prop) else default

    operator fun <T> set(prop: String, value: T) = props.put(prop, value)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MutableProps) return false

        if (player != other.player) return false
        if (props != other.props) return false

        return true
    }

    override fun hashCode(): Int {
        var result = player.hashCode()
        result = 31 * result + props.hashCode()
        return result
    }
}

//todo player
abstract class OldProps {

}

object BlankProps : OldProps()
