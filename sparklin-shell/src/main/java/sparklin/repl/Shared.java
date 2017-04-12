package sparklin.repl;

import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SQLContext;


/**
 * Created by vitaly.khudobakhshov on 12/04/17.
 */
public class Shared {
    public static JavaSparkContext sc = null;

    public static SQLContext sqlc = null;
}
