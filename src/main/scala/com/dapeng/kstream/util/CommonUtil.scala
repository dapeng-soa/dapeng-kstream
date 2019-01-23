package com.dapeng.kstream.util

import java.io.{File, FileInputStream}
import java.util.Properties
import collection.JavaConverters._

object CommonUtil {

  val LogMatcher = """(?<logtime>^\d{2}-\d{2} \d{2}:\d{2}:\d{2} \d{3}) (?<threadPool>[^ ]+|Check idle connection Thread) (?<level>[^ ]+) \[(?<sessionTid>\w*)\] - (?<message>.*)""".r

  def loadSystemProperties(file: File): Unit = {
    if (file.canRead) {
      val properties = new Properties()
      properties.load(new FileInputStream(file))

      val results = properties.keySet().asScala.map(_.toString)
      results.foreach(keyString => {
        System.setProperty(keyString, properties.getProperty(keyString))
      })
    }
  }

}
