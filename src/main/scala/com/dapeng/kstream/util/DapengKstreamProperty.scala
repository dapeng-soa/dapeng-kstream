package com.dapeng.kstream.util

import java.io.{File, FileNotFoundException}


object DapengKstreamProperty {

  {
    initEnv()
  }

  def initEnv() = {
    val path = System.getProperty("user.dir")
    val propertiesPath = path + File.separator + "kstreams.properties"
    val file = new File(propertiesPath)
    if (!file.exists()) {
      throw new FileNotFoundException("kstreams.properties not found.. please check")
    }
    CommonUtil.loadSystemProperties(file)
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
