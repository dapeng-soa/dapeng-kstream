package com.dapeng.kstream

import java.io.File

import com.dapeng.kstream.util.CommonUtil
import org.apache.kafka.streams.scala.Serdes
import org.apache.kafka.streams.scala.kstream.Consumed

trait AbstractBootstrap {

  val builder = new DapengInnerStreamBuilder[String, String](Consumed.`with`(Serdes.String, Serdes.String))
  import builder._

  def execute = {
    val kafkaServer = initProperties()
    if (kafkaServer == null && kafkaServer.isEmpty) {
      throw new IllegalArgumentException("kafka.server config not found.. pelase check kstreams.properties or sysEnv..")
    }
    build
    start(kafkaServer, "latest")
  }


  def initProperties() = {
    val file = new File("kstreams.properties")
    CommonUtil.loadSystemProperties(file)

    val serverEnv = System.getenv("kafka.server")
    if (serverEnv != null && ! serverEnv.isEmpty) {
      serverEnv
    } else {
      System.getProperty("kafka.server")
    }
  }

  def build
}
