package sparklin.core

/**
  * Created by vitaly.khudobakhshov on 11/04/17.
  */
class Logging extends org.apache.spark.Logging {
  def getLog() = log
}
