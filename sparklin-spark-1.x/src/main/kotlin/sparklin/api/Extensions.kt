package sparklin.api

import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.DataFrameReader
import org.apache.spark.sql.DataFrameWriter
import org.apache.spark.sql.SQLContext
import scala.Tuple2
import scala.Tuple3

val SQLContext.read: DataFrameReader
    get() = this.read()

val DataFrame.write: DataFrameWriter
    get() = this.write()

fun <T1, T2> tuple(_1: T1, _2: T2) = Tuple2(_1, _2)
fun <T1, T2, T3> tuple(_1: T1, _2: T2, _3: T3) = Tuple3(_1, _2, _3)