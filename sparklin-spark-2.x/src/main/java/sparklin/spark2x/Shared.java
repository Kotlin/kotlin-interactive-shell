package sparklin.spark2x;

import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;

public class Shared {
    public static JavaSparkContext sc = null;

    public static SparkSession spark = null;
}
