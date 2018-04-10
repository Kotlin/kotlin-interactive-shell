package sparklin.kshell.plugins

import sparklin.kshell.Plugin
import org.apache.hadoop.conf.Configuration

interface SparkPlugin : Plugin {
    fun hadoopConfiguration(): Configuration?
}