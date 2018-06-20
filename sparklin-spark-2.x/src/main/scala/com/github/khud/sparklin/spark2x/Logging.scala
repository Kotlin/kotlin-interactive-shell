package com.github.khud.sparklin.spark2x

class Logging extends org.apache.spark.internal.Logging {
  def getLog() = log
  def logInfo(msg: String) = super.logInfo { msg }
}