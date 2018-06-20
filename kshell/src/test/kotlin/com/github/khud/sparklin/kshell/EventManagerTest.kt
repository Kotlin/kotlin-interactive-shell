package com.github.khud.sparklin.kshell

import org.junit.Test

import org.junit.Assert.*

class EventManagerTest {
    class MyEvent0(private val str: String) : Event<String> {
        override fun data(): String = str
    }

    class MyEvent1(private val str: String) : Event<String> {
        override fun data(): String = str
    }

    @Test
    fun consistencyTest() {
        val arr = arrayOf("", "")
        val em = EventManager()

        em.registerEventHandler(MyEvent0::class, object : EventHandler<MyEvent0> {
            override fun handle(event: MyEvent0) {
                arr[0] = event.data()
            }
        })

        em.registerEventHandler(MyEvent1::class, object : EventHandler<MyEvent1> {
            override fun handle(event: MyEvent1) {
                arr[1] = event.data()
            }
        })

        em.emitEvent(MyEvent0("hello"))
        em.emitEvent(MyEvent1("world"))

        assertEquals("hello", arr[0])
        assertEquals("world", arr[1])
    }
}