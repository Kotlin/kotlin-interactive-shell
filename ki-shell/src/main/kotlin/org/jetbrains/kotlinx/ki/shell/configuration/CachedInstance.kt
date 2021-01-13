package org.jetbrains.kotlinx.ki.shell.configuration

import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KClass

class CachedInstance<T: Any> {
    val cache = AtomicReference<T>(null)

    inline fun <reified X: T> load(klassName: String, type: KClass<X>): X {
        val cached = cache.get()
        return if (cached == null) {
            val klass = this.javaClass.classLoader.loadClass(klassName)
            val result = klass.newInstance() as X
            cache.set(result)
            result
        } else cached as X
    }

    fun get(init: () -> T): T {
        val cached = cache.get()
        return if (cached == null) {
            val value = init()
            cache.set(value)
            value
        } else cached
    }

    fun get(): T? = cache.get()
}