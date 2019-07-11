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
    /*      topic("test")
            .dapengFilter((_,v) => v.contains("ERROR") || v.contains("Exception"))
            .dapengMap((k,v) => {
              println(s" received msg: ${v}")
              (k, s"订单异常，请注意: $v") })
            .windowAlert(Duration.ofMinutes(1), "ERROR",
              2,"all",
              "orderService", "[订单错误统计告警]")
            .timeRangeAlert(2,
                6,
                Duration.ofMinutes(1), "ERROR",
                2,
                "all",
                "orderService",
                "[订单错误统计告警]")
    //        .sendMail("bbliang@today36524.com.cn", "订单异常")
    //        .sendDingding("18588733858")

        start("172.18.110.145:9092", "latest")*/


    /*    topic("dapeng-kstream-test")
          .dapengFilter((_, v) => v.contains("ERROR") || v.contains("Exception"))
          .dapengMap((k,v) => {
          println(s" received msg: $v")
          (k, s"错误异常，请注意: $v") })
          .windowAlert(Duration.ofMinutes(1),"ERROR",10,"dingding","kstreamTest","错误异常告警")
          .sendMail("kstreamTest", "错误异常告警")
          //.sendDingding("kstreamTest", (k, v) => (k, v))*/

    topic("dapeng-kstream-test","consumer")
      .dapengFilter((_, v) => v.contains("::startup end") || v.contains(" ------------ SpringClassLoader:"))
/*      .dapengMap((k,v) => {
        (k, s"错误异常，请注意: $v")
      })
      .windowAlert(Duration.ofMinutes(1),"startup",1,"dingding","kstreamTest","启动告警")*/
      .sendDingding("kstreamTest", (k,v: String) => {
        val msg = s" 产生自定义日志告警，日志信息为: ${String.valueOf(v)}"
        (k, msg)
      },"BUSINESS")



    start("192.168.4.221:9092", "latest")
  }
}
