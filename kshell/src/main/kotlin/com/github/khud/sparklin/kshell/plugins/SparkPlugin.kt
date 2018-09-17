package com.github.khud.sparklin.kshell.plugins

import com.github.khud.sparklin.kshell.Plugin
import org.apache.hadoop.conf.Configuration

interface SparkPlugin : Plugin {
    fun hadoopConfiguration(): Configuration?

    fun getSparkVersion(): String

    override fun sayHello() {
        println("Apache Spark ${getSparkVersion()}")
    }
}