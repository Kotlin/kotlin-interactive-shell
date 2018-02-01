package sparklin.spark1x

import kshell.EventHandler
import kshell.OnCompile
import kshell.Plugin
import kshell.Repl
import kshell.configuration.Configuration
import org.apache.spark.HttpServer
import org.apache.spark.SecurityManager
import org.apache.spark.SparkConf
import org.apache.spark.api.java.JavaSparkContext
import org.apache.spark.sql.SQLContext
import org.apache.spark.util.Utils
import sparklin.core.Logging
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream

class Spark1xPlugin : Logging(), Plugin {
    private lateinit var classServer: HttpServer

    override fun init(repl: Repl, config: Configuration) {
        val jars = getAddedJars()
        val conf = SparkConf()
                .setMaster(getMaster())
                .setJars(jars.toTypedArray())
                .setAppName("Kotlin Spark Shell")

        val tmp = System.getProperty("java.io.tmpdir")
        val rootDir = conf.get("spark.repl.classdir", tmp)
        val classServerPort = conf.getInt("spark.replClassServer.port", 0)
        val outputDir = Utils.createTempDir(rootDir, "spark")
        classServer = HttpServer(conf, outputDir, SecurityManager(conf), classServerPort, "HTTP class server")
        classServer.start()
        Shared.sc = createSparkContext(conf, classServer)
        Shared.sqlContext = createSQLContext(Shared.sc)
        val replJars = replJars(jars)

        repl.registerEventHandler(OnCompile::class, object : EventHandler<OnCompile> {
            override fun handle(event: OnCompile) {
                event.data().classes.forEach {
                    writeClass(outputDir.absolutePath + File.separator + it.path, it.bytes)
                }
            }
        })

        repl.addClasspathRoots(replJars)
        repl.addImports(listOf(Shared::class.qualifiedName!! + ".*"))
    }

    override fun cleanUp() {
        classServer.stop()
    }

    private fun getAddedJars(): List<String> {
        val jars = System.getProperty("spark.jars")
        return Utils.resolveURIs(jars).split(",").filter { s -> s.trim() != "" }
    }

    private fun replJars(jars: List<String>): List<File> {
        // to get Spark related libraries and so on
        val jvmClasspath = System.getProperty("java.class.path")
        val systemJars = jvmClasspath.split(File.pathSeparatorChar).map(::File)

        return jars.filter {
                // exclude all Kotlin stuff from REPL classpath
                it -> !it.contains(Regex("/kotlin-(runtime|stdlib|compiler|reflect)(-.*)?\\.jar"))
            }.map {
                // remove "file:"
                it ->
                val p = it.indexOf(':')
                File(it.substring(p + 1))
            } + systemJars
    }

    private fun createSparkContext(conf: SparkConf, classServer: HttpServer): JavaSparkContext {
        val execUri = System.getenv("SPARK_EXECUTOR_URI")
        conf.set("spark.repl.class.uri", classServer.uri())
                .setIfMissing("spark.app.name", "Kotlin Spark Shell")

        log.info("Spark class server started at " + classServer.uri())
        if (execUri != null) {
            conf.set("spark.executor.uri", execUri)
        }
        if (System.getenv("SPARK_HOME") != null) {
            conf.setSparkHome(System.getenv("SPARK_HOME"))
        }
        val sparkContext = JavaSparkContext(conf)
        log.info("Created spark context..")

        return sparkContext
    }

    private fun createSQLContext(sparkContext: JavaSparkContext): SQLContext {
        val name = "org.apache.spark.sql.hive.HiveContext"
        val loader = Utils.getContextOrSparkClassLoader()
        try {
            val sqlContext = loader.loadClass(name)
                    .getConstructor(JavaSparkContext::class.java)
                    .newInstance(sparkContext) as SQLContext
            log.info("Created sql context (with Hive support)..")
            return sqlContext
        } catch (e: Throwable) {
            when (e) {
                is java.lang.ClassNotFoundException, is java.lang.NoClassDefFoundError -> {
                    val sqlContext = SQLContext(sparkContext)
                    log.info("Created sql context..")
                    return sqlContext
                }
                else -> throw e
            }
        }
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
}