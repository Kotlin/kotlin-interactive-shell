package sparklin

import org.apache.spark.api.java.JavaSparkContext
import org.apache.spark.sql.SQLContext
import scala.Tuple2
import sparklin.api.read
import sparklin.api.tuple as T

fun <K, V> JavaSparkContext.parallelize(list: List<Tuple2<K, V>>) {

}

fun test(sc: JavaSparkContext, sqlContext: SQLContext) {
    sqlContext.read.parquet()
    val t2 = T(1,2)
    val t3 = T(1,2,3)
    val q = sc.parallelize(listOf(T(1,2), T(1,3), T(2, 2)))
    q.mapToPair { it }
}

class A(i: Int) {
    operator fun plus(a: A): A {
        TODO()
    }
}

fun f() {
    val a = A(10)
    val b = A(20)
    a + b
}