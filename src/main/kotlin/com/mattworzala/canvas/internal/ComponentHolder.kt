package com.mattworzala.canvas.internal

import com.mattworzala.canvas.Component
import com.mattworzala.canvas.FunctionComponent
import com.mattworzala.canvas.Props
import com.mattworzala.canvas.SlotHolder
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import java.util.*

abstract class ComponentHolder : SlotHolder {
    private val children: Int2ObjectMap<Component<*>> = Int2ObjectOpenHashMap()
    internal val cleanupTasks: MutableList<Runnable> = mutableListOf()

    //this has a memory leak if components are rapidly changed (and set to the same index)
    open fun <P : Props> child(index: Int, component: FunctionComponent<P>, props: P, propHandler: P.() -> Unit) {
        val childId = Objects.hash(index, component)

        @Suppress("UNCHECKED_CAST")
        val child: Component<P> =
            children.computeIfAbsent(childId) { Component(this, index, component) } as Component<P>
        props.propHandler()
        child.render(props)
    }

    open fun update(): Unit = children.values.forEach(ComponentHolder::update)

    internal fun cleanup() {
        children.values.forEach(ComponentHolder::cleanup)
        cleanupTasks.forEach(Runnable::run)
    }
}