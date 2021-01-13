package org.jetbrains.kotlinx.ki.shell

import kotlin.reflect.KClass

class EventManager {
    private val eventHandlers = hashMapOf<String, MutableList<EventHandler<Any>>>()

    fun <T> emitEvent(event: Event<T>) {
        eventHandlers[event.javaClass.kotlin.qualifiedName]?.let {
            it.forEach {
                it.handle(event)
            }
        }
    }

    fun <E : Any> registerEventHandler(eventType: KClass<E>, handler: EventHandler<E>) {
        @Suppress("UNCHECKED_CAST")
        eventHandlers.getOrPut(eventType.qualifiedName!!, { mutableListOf() }).add(handler as EventHandler<Any>)
    }
}

interface EventHandler<E> {
    fun handle(event: E)
}

interface Event<T> {
    fun data(): T
}