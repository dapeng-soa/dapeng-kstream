package com.lbb.test

import java.time.Duration

import com.dapeng.kstream.AbstractBootstrap
import com.dapeng.kstream.pojo.GcInfo
import com.google.gson.Gson
import org.apache.kafka.streams.kstream.internals.DapengKStreamEnhancer._
import org.apache.kafka.streams.scala.Serdes
import org.apache.kafka.streams.scala.kstream.{Grouped, KStream, Materialized}

class BootstrapImpl extends AbstractBootstrap{

  override def build: Unit = {
    println(" start to build topology......")
    import builder._

    topic("gcinfo")
      .dapengFilter((k,v) => v.contains("Full Gc"))
      .groupBy((k,v) => {
        val matcher ="""(?<serviceName>^[a-zA-Z]+) (?<logtime>\d{2}-\d{2} \d{2}:\d{2}:\d{2} \d{3}) (?<threadPool>[^ ]+|Check idle connection Thread) (?<level>[^ ]+) \[(?<sessionTid>\w*)\] - (?<message>.*)""".r
        val matcher(serviceName, _, _, _, _, _) = v
        serviceName
      })(Grouped.`with`(Serdes.String, Serdes.String))
      .window(Duration.ofMinutes(1), 2)
      .sendDingding("orderService",
        (k,v: String) => {
        val msg = s" Full Gc告警， service: ${k}, expect: 2, actual: ${String.valueOf(v)}"
        (k, msg)
      },"BUSINESS")
  }
}
