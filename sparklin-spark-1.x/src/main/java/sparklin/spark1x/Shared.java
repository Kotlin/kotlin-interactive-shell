package sparklin.spark1x;

import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SQLContext;

public class Shared {
    public static JavaSparkContext sc = null;

    public static SQLContext sqlContext = null;
}