package sparklin

import org.apache.spark.api.java.JavaSparkContext
import org.apache.spark.sql.RowFactory
import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.types.DataTypes.*
import scala.Tuple2

fun wordCount(sc: JavaSparkContext, sqlContext: SQLContext) {
    val textFile = sc.textFile("/Users/vitaly.khudobakhshov/Downloads/onegin_utf8.txt")

    val schema = createStructType(listOf(
            createStructField("word", StringType, false),
            createStructField("count", IntegerType, false)))

    val counts = textFile.flatMap { line -> line.split(" ") }
            .mapToPair { word -> Tuple2(word.trim(), 1) }
            .reduceByKey(Int::plus)
            .map { RowFactory.create(it._1, it._2) }

    val df = sqlContext.createDataFrame(counts, schema)
}

