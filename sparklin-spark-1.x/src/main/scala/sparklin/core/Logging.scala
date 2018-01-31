package sparklin.core

class Logging extends org.apache.spark.Logging {
  def getLog() = log
}