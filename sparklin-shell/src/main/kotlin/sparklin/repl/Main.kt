package sparklin.repl

import kshell.command.Help
import kshell.command.Load
import kshell.command.Type
import lib.jline.console.history.FileHistory
import org.apache.spark.HttpServer
import org.apache.spark.SecurityManager
import org.apache.spark.SparkConf
import org.apache.spark.api.java.JavaSparkContext
import org.apache.spark.sql.SQLContext
import org.apache.spark.util.Utils
import sparklin.core.Logging
import java.io.File

import sparklin.repl.Shared.*

/**
 * Created by vitaly.khudobakhshov on 15/03/17.
 */
object Main: Logging() {
    @JvmStatic
    fun main(args: Array<String>) {
        val jars = getAddedJars()
        val conf = SparkConf()
                .setMaster(getMaster())
                .setJars(jars.toTypedArray())
                .setAppName("Kotlin Spark Shell")

        val tmp = System.getProperty("java.io.tmpdir")
        val rootDir = conf.get("spark.repl.classdir", tmp)
        val classServerPort = conf.getInt("spark.replClassServer.port", 0)
        val outputDir = Utils.createTempDir(rootDir, "spark")
        val classServer = HttpServer(conf, outputDir, SecurityManager(conf), classServerPort, "HTTP class server")
        classServer.start()
        sc = createSparkContext(conf, classServer)
        sqlc = createSQLContext(sc)
        val replJars = replJars(jars)
        val historyPath = System.getProperty("user.home") + File.separator + ".sparklin.history"

        val hist = FileHistory(File(historyPath))
        val repl = SparkRepl(additionalClasspath = replJars, classesOutputDir = outputDir, history = hist)
        Runtime.getRuntime().addShutdownHook(Thread({
            println("\nBye!")
            repl.cleanUp()
        }))
        repl.apply {
            registerCommand(Help())
            registerCommand(Load())
            registerCommand(Type())
            doRun()
        }
        classServer.stop()
    }

    fun getAddedJars(): List<String> {
        val jars = System.getProperty("spark.jars")
        return Utils.resolveURIs(jars).split(",").filter { s -> s.trim() != "" }
    }

    fun replJars(jars: List<String>): List<File> {
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

    fun createSparkContext(conf: SparkConf, classServer: HttpServer): JavaSparkContext {
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

    fun createSQLContext(sparkContext: JavaSparkContext): SQLContext {
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

    fun getMaster(): String {
        val propMaster = System.getProperty("spark.master")
        return propMaster ?: System.getenv()["MASTER"] ?: "local[*]"
    }
}