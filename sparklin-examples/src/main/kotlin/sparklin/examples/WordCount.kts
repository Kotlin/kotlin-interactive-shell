import scala.Tuple2
import sparklin.repl.Env.*


val textFile = sc.textFile("examples/text")

val counts = textFile.flatMap { line -> line.split(" ") }.
        mapToPair { word -> Tuple2(word, 1) }.
        reduceByKey { x, y -> x + y }

counts.saveAsTextFile("examples/wordcount")


