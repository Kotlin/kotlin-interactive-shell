package org.jetbrains.kotlinx.ki.shell

import java.util.*
import kotlin.NoSuchElementException

object ApplicationProperties {
    private val props: Properties = Properties()

    val version: String
        get() = get("project.version")

    fun get(key: String): String {
        if (props.isEmpty) {
            val inputStream = javaClass.getResourceAsStream("/application.properties")
            inputStream.use {
                props.load(inputStream)
            }
        }
        return props.getProperty(key) ?: throw NoSuchElementException(key)
    }
}