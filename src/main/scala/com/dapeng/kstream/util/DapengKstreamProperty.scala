package com.dapeng.kstream.util

import java.io.File

object DapengKstreamProperty {

  {
    CommonUtil.loadSystemProperties(new File("kstreams.properties"))
  }

  val KEY_DAPENG_KSTREAM_WORK_HOME = "dapeng.kstream.work.home"
  val KEY_KAFKA_SERVER = "kafka.server"

  val DAPENG_STREAM_WORK_HOME = getConfig(KEY_DAPENG_KSTREAM_WORK_HOME,"")
  val KAFKA_SERVER = getConfig(KEY_KAFKA_SERVER,"")


  private def getConfig(propertyName: String, defaultValue: String) = {
    val envValue = System.getenv(propertyName.replaceAll("\\.", "_"))
    if (envValue == null) {
      System.getProperty(propertyName, defaultValue)
    } else {
      envValue
    }
  }
}
