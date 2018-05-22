package sparklin.spark2x

import sparklin.kshell.configuration.Configuration
import org.apache.spark.SparkConf
import org.apache.spark.api.java.JavaSparkContext
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.internal.StaticSQLConf.CATALOG_IMPLEMENTATION
import org.apache.spark.util.Utils
import sparklin.core.Logging
import sparklin.kshell.*
import sparklin.kshell.plugins.SparkPlugin
import com.github.khud.kshell.repl.SyntheticImportSnippet
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.*

class Spark2xPlugin : Logging(), SparkPlugin {
    override fun init(repl: KShell, config: Configuration) {
        val jars = getAddedJars()
        val conf = SparkConf()
                .setMaster(getMaster())
                .setJars(jars.toTypedArray())
                .setAppName("Kotlin Spark Shell")

        val tmp = System.getProperty("java.io.tmpdir")
        val rootDir = conf.get("spark.repl.classdir", tmp)
        val outputDir = Utils.createTempDir(rootDir, "spark")
        Holder.sparkSessionDelegate = createSparkSession(conf, outputDir)
        Holder.sparkContextDelegate = JavaSparkContext.fromSparkContext(Holder.sparkSessionDelegate.sparkContext())
        val replJars = replJars(jars)

        repl.eventManager.registerEventHandler(OnCompile::class, object : EventHandler<OnCompile> {
            override fun handle(event: OnCompile) {
                event.data().classes.classes.forEach {
                    writeClass(outputDir.absolutePath + File.separator + it.path, it.bytes)
                }
            }
        })

        repl.addClasspathRoots(replJars)
        repl.state.history.add(SyntheticImportSnippet(Holder::class.qualifiedName!!, "sc", "sc"))
        repl.state.history.add(SyntheticImportSnippet(Holder::class.qualifiedName!!, "spark", "spark"))
    }

    object Holder {
        val spark: SparkSession by lazy { sparkSessionDelegate }
        val sc: JavaSparkContext by lazy { sparkContextDelegate }

        lateinit var sparkSessionDelegate: SparkSession
        lateinit var sparkContextDelegate: JavaSparkContext
    }

    private fun createSparkSession(conf: SparkConf, outputDir: File): SparkSession {
        val execUri = System.getenv("SPARK_EXECUTOR_URI")
        var sparkSession: SparkSession
        conf.setIfMissing("spark.app.name", "Spark shell")
        // SparkContext will detect this configuration and register it with the RpcEnv's
        // file server, setting spark.repl.class.uri to the actual URI for executors to
        // use. This is sort of ugly but since executors are started as part of SparkContext
        // initialization in certain cases, there's an initialization order issue that prevents
        // this from being set after SparkContext is instantiated.
        conf.set("spark.repl.class.outputDir", outputDir.getAbsolutePath())
        if (execUri != null) {
            conf.set("spark.executor.uri", execUri)
        }
        if (System.getenv("SPARK_HOME") != null) {
            conf.setSparkHome(System.getenv("SPARK_HOME"))
        }

        val builder = SparkSession.builder().config(conf)
        if (conf.get(CATALOG_IMPLEMENTATION().key(), "hive").toLowerCase(Locale.ROOT) == "hive") {
            if (hiveClassesArePresent()) {
                // In the case that the property is not set at all, builder's config
                // does not have this value set to 'hive' yet. The original default
                // behavior is that when there are hive classes, we use hive catalog.
                sparkSession = builder.enableHiveSupport().getOrCreate()
                logInfo("Created Spark session with Hive support")
            } else {
                // Need to change it back to 'in-memory' if no hive classes are found
                // in the case that the property is set to hive in spark-defaults.conf
                builder.config(CATALOG_IMPLEMENTATION().key(), "in-memory")
                sparkSession = builder.getOrCreate()
                logInfo("Created Spark session")
            }
        } else {
            // In the case that the property is set but not to 'hive', the internal
            // default is 'in-memory'. So the sparkSession will use in-memory catalog.
            sparkSession = builder.getOrCreate()
            logInfo("Created Spark session")
        }
        return sparkSession
    }

    override fun cleanUp() { }

    private fun getAddedJars(): List<String> {
        val jars = System.getProperty("spark.jars")
        return Utils.resolveURIs(jars).split(",").filter { s -> s.trim() != "" }
    }

    private fun getMaster(): String {
        val propMaster = System.getProperty("spark.master")
        return propMaster ?: System.getenv()["MASTER"] ?: "local[*]"
    }

    private fun writeClass(path: String, bytes: ByteArray) {
        val out = BufferedOutputStream(FileOutputStream(path))
        out.write(bytes)
        out.flush()
        out.close()
    }

    private fun hiveClassesArePresent(): Boolean {
        return try {
            Utils.classForName("org.apache.spark.sql.hive.HiveSessionStateBuilder")
            Utils.classForName("org.apache.hadoop.hive.conf.HiveConf")
            true
        } catch (e: Exception) {
            when (e) {
                is ClassNotFoundException, is NoClassDefFoundError -> false
                else -> throw e
            }
        }
    }

    override fun hadoopConfiguration() = Holder.sc.hadoopConfiguration()
}