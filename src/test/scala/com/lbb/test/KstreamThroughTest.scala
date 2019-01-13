package com.lbb.test

import java.time.Duration
import java.util.{Properties, UUID}

import com.dapeng.kstream.DapengInnerStreamBuilder
import org.apache.kafka.streams.scala.kstream.Consumed
import org.apache.kafka.streams.scala.{Serdes}
import org.apache.kafka.streams.kstream.internals.DapengKStreamEnhancer._

object KstreamThroughTest {

  def main(args: Array[String]): Unit = {

    val builder = new DapengInnerStreamBuilder[String, String](Consumed.`with`(Serdes.String, Serdes.String))

    import builder._
      topic("test")
        .dapengFilter((_,v) => v.contains("ERROR") || v.contains("Exception"))
        .dapengMap((k,v) => (k, s"订单异常，请注意: $v") )
        .clockCountToWarn(Duration.ofMinutes(1), "ERROR", 2)
        .clockToClockCountToWarn(3,4, Duration.ofMinutes(1), "ERROR", 2)
        .sendMail("bbliang@today36524.com.cn", "订单异常")
        .sendDingding("18588733858")

    start("172.18.110.145:9092", "latest")

  }
}
